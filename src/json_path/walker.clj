(ns json-path.walker)

(declare walk eval-expr)

(defn eval-eq-expr [op-form context operands]
  (apply op-form (map #(eval-expr % context) operands)))

(defn eval-expr [[expr-type & operands :as expr] context]
  (let [ops {:eq =, :neq not=, :lt <, :lt-eq <=, :gt >, :gt-eq >=}]
    (cond
     (contains? ops expr-type) (eval-eq-expr (expr-type ops) context operands)
     (= expr-type :val) (first operands)
     (= expr-type :path) (walk expr context))))

(defn select-by [[opcode & operands :as obj-spec] context]
  (cond
   (sequential? (:current context)) (vec (flatten (filter #(not (empty? %))
                                                          (map #(select-by obj-spec (assoc context :current %))
                                                               (:current context)))))
   :else (cond
          (= (first operands) "*") (vec (vals (:current context)))
          :else ((keyword (first operands)) (:current context)))))

(defn obj-aggregator [obj]
  (let [obj-vals (vec (filter map? (vals obj)))
        children (flatten (map obj-aggregator obj-vals))]
    (vec (concat obj-vals children))))

(defn walk-path [[next & parts] context]
  (cond
   (nil? next) (:current context)
   (= [:root] next) (walk-path parts (assoc context :current (:root context)))
   (= [:child] next) (walk-path parts context)
   (= [:current] next) (walk-path parts context)
   (= [:all-children] next) (walk-path parts (assoc context :current (vec (concat [(:current context)]
                                                                                  (obj-aggregator (:current context))))))
   (= :key (first next)) (walk-path parts (assoc context :current (select-by next context)))))

(defn walk-selector [sel-expr context]
  (cond
   (= :index (first sel-expr)) (if (sequential? (:current context))
                                 (let [sel (nth sel-expr 1)]
                                   (if (= "*" sel)
                                     (:current context)
                                     (nth (:current context) (Integer/parseInt sel)))))
   (= :filter (first sel-expr)) (filter #(eval-expr (nth sel-expr 1) (assoc context :current %)) (:current context))))

(defn walk [[opcode operand continuation] context]
  (let [down-obj (cond
         (= opcode :path) (walk-path operand context)
         (= opcode :selector) (walk-selector operand context))]
    (if continuation
      (walk continuation (assoc context :current down-obj))
      down-obj)))
