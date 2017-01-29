(ns json-path.walker
  [:require [json-path.match :as m]])

(declare walk eval-expr)

(defn eval-eq-expr [op-form context operands]
  (apply op-form (map #(eval-expr % context) operands)))

(defn eval-expr [[expr-type & operands :as expr] context]
  (let [ops {:eq =, :neq not=, :lt <, :lt-eq <=, :gt >, :gt-eq >=}]
    (cond
     (contains? ops expr-type) (eval-eq-expr (expr-type ops) context operands)
     (= expr-type :val) (first operands)
     (= expr-type :path) (:value (walk expr context)))))

(defn map# [func obj]
  (if (seq? obj)
    (flatten (map func obj))
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
                                 (remove #(empty? (:value %))))
   (= :key (first next)) (map# #(walk-path parts (assoc context :current %)) (select-by next (:current context)))))

(defn walk-selector [sel-expr context]
  (cond
   (= :index (first sel-expr)) (let [obj (:value (:current context))
                                     sel (nth sel-expr 1)]
                                 (if (= "*" sel)
                                   (select-all (:current context))
                                   (if (sequential? obj)
                                     (let [index (Integer/parseInt sel)]
                                       (m/with-context index (nth obj index) (:current context)))
                                     (throw (Exception. "object must be an array.")))))
   (= :filter (first sel-expr)) (keep-indexed (fn [i e] (if (eval-expr (nth sel-expr 1) (assoc context :current (m/root e)))
                                                          (m/with-context i e (:current context))))
                                              (:value (:current context)))))

(defn walk [[opcode operand continuation] context]
  (let [down-obj (cond
         (= opcode :path) (walk-path operand context)
         (= opcode :selector) (walk-selector operand context))]
    (if continuation
      (map# #(walk continuation (assoc context :current %)) down-obj)
      down-obj)))
