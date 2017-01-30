(ns json-path.walker)

(declare walk eval-expr)

(defn map# [func obj]
  (if (seq? obj)
    (map func obj)
    (func obj)))

(defn eval-eq-expr [op-form context operands]
  (apply op-form (map #(eval-expr % context) operands)))

(defn eval-expr [[expr-type & operands :as expr] context]
  (let [ops {:eq =, :neq not=, :lt <, :lt-eq <=, :gt >, :gt-eq >=}]
    (cond
     (contains? ops expr-type) (eval-eq-expr (expr-type ops) context operands)
     (= expr-type :val) (first operands)
     (= expr-type :path) (first (walk expr context)))))

(defn- with-parent-key [parent-key selection]
  (map# (fn [[value key]] [value (vec (concat parent-key key))]) selection))

(defn- map-selection [func selection]
  (let [sub-selection (map# (fn [[val key]] (with-parent-key key (func val))) selection)]
    (if (seq? sub-selection)
      (->> sub-selection
           (reduce (fn [col item] (if (seq? item)
                                    (concat col item)
                                    (conj col item)))
                   [])
           seq)
      sub-selection)))

(defn select-by [[opcode & operands :as obj-spec] context]
  (cond
   (sequential? (:current context)) (let [sub-selection (->> (:current context)
                                                             (map #(select-by obj-spec (assoc context :current %)))
                                                             (map-indexed (fn [i sel] (map# (fn [[obj key]] [obj (vec (cons i key))]) sel)))
                                                             (filter #(not (empty? (first %)))))]
                                      (if (seq? (first sub-selection))
                                        (apply concat sub-selection)
                                        sub-selection))
   :else (cond
          (= (first operands) "*") (map (fn [[k v]] [v [k]]) (:current context))
          :else (let [key (keyword (first operands))]
                  [(key (:current context)) [key]]))))

(defn obj-vals [obj]
  (cond
    (seq? obj) (map-indexed (fn [idx child-obj] [child-obj [idx]]) obj)
    (map? obj) (->> obj
                    (filter (fn [[k v]] (or (map? v) (sequential? v))))
                    (map (fn [[k v]] [v [k]])))
    :else '()))

(defn all-children [obj]
  (let [children (map-selection all-children (obj-vals obj))]
    (concat (list [obj []]) children)))

(defn walk-path [[next & parts] context]
  (cond
   (nil? next) [(:current context) []]
   (= [:root] next) (walk-path parts (assoc context :current (:root context)))
   (= [:child] next) (walk-path parts context)
   (= [:current] next) (walk-path parts context)
   (= [:all-children] next) (->> (:current context)
                                 all-children
                                 (map-selection #(walk-path parts (assoc context :current %)))
                                 (filter #(not (empty? (first %)))))
   (= :key (first next)) (map-selection #(walk-path parts (assoc context :current %)) (select-by next context))))

(defn walk-selector [sel-expr context]
  (cond
   (= :index (first sel-expr)) (if (sequential? (:current context))
                                 (let [sel (nth sel-expr 1)]
                                   (if (= "*" sel)
                                     (map-indexed (fn [idx child-obj] [child-obj [idx]]) (:current context))
                                     (let [index (Integer/parseInt sel)]
                                       [(nth (:current context) index) [index]])))
                                 (throw (Exception. "object must be an array.")))
   (= :filter (first sel-expr)) (keep-indexed (fn [i e] (if (eval-expr (nth sel-expr 1) (assoc context :current e)) [e [i]]))
                                              (:current context))))

(defn walk [[opcode operand continuation] context]
  (let [down-obj (cond
         (= opcode :path) (walk-path operand context)
         (= opcode :selector) (walk-selector operand context))]
    (if continuation
      (map-selection #(walk continuation (assoc context :current %)) down-obj)
      down-obj)))
