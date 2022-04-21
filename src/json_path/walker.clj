(ns json-path.walker
  [:require [json-path.match :as m]])

(declare walk eval-expr)

(defn map-with-value? [m]
  (and (map? m)
       (contains? m :value)))

;; 'and' and 'or' are macros in Clojure, so we wrap them in functions here so they
;; can be passed to apply and tested for equality
(defn and* [a b]
  (and a b))

(defn or* [a b]
  (or a b))

(defn eval-bool-expr [comp-fn context operands]
  (boolean
    (apply
      (fn [a b]
        (cond
          ;; allow less-than, greater-than comparisons if both args are Numbers or Strings
          (and (contains? #{< <= > >=} comp-fn)
               (or (and (number? a) (number? b))
                   (and (string? a) (string? b))))
          (comp-fn a b)

          ;; always allow equality comparisons as well as 'and' and 'or'
          (contains? #{= not= and* or*} comp-fn)
          (comp-fn a b)

          ;; else the two values cannot be compared: return false
          :else false))
      (map #(eval-expr % context) operands))))

(def boolean-ops
  "expression operands that result in a boolean result"
  {:eq =
   :neq not=
   :lt <
   :lt-eq <=
   :gt >
   :gt-eq >=
   ;; NOTE: see comment above, these macros are wrapped as functions
   :and and*
   :or or*})

(defn eval-expr [[expr-type & operands :as expr] context]
  (cond
   (contains? boolean-ops expr-type) (eval-bool-expr (get boolean-ops expr-type) context operands)
   (= expr-type :bool) (let [inner-val (walk (first operands) context)]
                         (if (map-with-value? inner-val)
                           (:value inner-val)
                           inner-val))
   (= expr-type :val) (first operands)
   (= expr-type :path) (:value (walk expr context))))

(defn map# [func obj]
  (if (seq? obj)
    (->> obj
         (flatten)
         (map (partial map# func)))
    (func obj)))

(defn- select-all [current-context]
  (let [obj (:value current-context)]
    (cond (map? obj) (map (fn [[k v]] (m/with-context k v current-context)) obj)
          (sequential? obj) (map-indexed (fn [idx child-obj] (m/with-context idx child-obj current-context)) obj)
          :else '())))

(defn select-by [[opcode & operands :as obj-spec] current-context]
  (if (= (first operands) "*")
    (select-all current-context)
    (let [obj (:value current-context)
          key (keyword (first operands))]
      (m/with-context key (key obj) current-context))))

(defn- obj-vals [current-context]
  (let [obj (:value current-context)]
    (cond
      (sequential? obj) (map-indexed (fn [idx child-obj] (m/with-context idx child-obj current-context)) obj)
      (map? obj) (->> obj
                      (filter (fn [[k v]] (or (map? v) (sequential? v))))
                      (map (fn [[k v]] (m/with-context k v current-context))))
      :else '())))

(defn- all-children [current-context]
  (let [children (mapcat all-children (obj-vals current-context))]
    (cons current-context children)))

(defn walk-path [[next & parts] context]
  (cond
   (nil? next) (:current context)
   (= [:root] next) (walk-path parts (assoc context :current (:root context)))
   (= [:child] next) (walk-path parts context)
   (= [:current] next) (walk-path parts context)
   (= [:all-children] next) (->> (:current context)
                                 all-children
                                 (map #(walk-path parts (assoc context :current %)))
                                 flatten
                                 (remove #(nil? (:value %))))
   (= :key (first next)) (map# #(walk-path parts (assoc context :current %)) (select-by next (:current context)))))

(defn walk-selector [sel-expr context]
  (cond
   (= :index (first sel-expr)) (let [obj (:value (:current context))
                                     sel (nth sel-expr 1)]
                                 (if (= "*" sel)
                                   (select-all (:current context))
                                   (if (sequential? obj)
                                     (let [index (Integer/parseInt sel)
                                           effective-index (if (< index 0)
                                                             (+ (count obj) index)
                                                             index)]
                                       (m/with-context index (nth obj effective-index) (:current context)))
                                     (throw (Exception. "object must be an array.")))))
   (= :filter (first sel-expr)) (let [obj (:value (:current context))
                                      children (if (map? obj)
                                                 (map identity obj)
                                                 (map-indexed (fn [i e] [i e]) obj))]
                                  (->> children
                                       (filter (fn [[key val]] (eval-expr (second sel-expr) (assoc context :current (m/root val)))))
                                       (map (fn [[key val]] (m/with-context key val (:current context))))))))

(defn walk [[opcode operand continuation :as expr] context]
  (let [down-obj (cond
                   (= opcode :path) (walk-path operand context)
                   (= opcode :selector) (walk-selector operand context)
                   (= opcode :val) (eval-expr expr context)
                   :else nil)]
    (if continuation
      (map# #(walk continuation (assoc context :current %)) down-obj)
      down-obj)))
