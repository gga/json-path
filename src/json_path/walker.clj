(ns json-path.walker)

(declare walk)

(defn eval-expr [[expr-type & operands :as expr] object]
  (cond
   (= expr-type :eq) (apply = (map #(eval-expr % object) operands))
   (= expr-type :val) (first operands)
   (= expr-type :path) (walk expr object)))

(defn select-by [[opcode & operands :as obj-spec] object]
  (cond
   (sequential? object) (vec (flatten (filter #(not (empty? %))
                                              (map #(select-by obj-spec %) object))))
   :else (cond
          (= (first operands) "*") (vec (vals object))
          :else ((keyword (first operands)) object))))

(defn obj-aggregator [obj]
  (let [obj-vals (vec (filter map? (vals obj)))
        children (flatten (map obj-aggregator obj-vals))]
    (vec (concat obj-vals children))))

(defn walk-path [[next & parts] object]
  (cond
   (nil? next) object
   (= [:root] next) (walk-path parts object)
   (= [:child] next) (walk-path parts object)
   (= [:current] next) (walk-path parts object)
   (= [:all-children] next) (walk-path parts (vec (concat [object] (obj-aggregator object))))
   (= :key (first next)) (walk-path parts (select-by next object))))

(defn walk-selector [sel-expr object]
  (cond
   (= :index (first sel-expr)) (let [sel (nth sel-expr 1)]
                                 (if (= "*" sel)
                                   object
                                   (nth object (Integer/parseInt sel))))
   (= :filter (first sel-expr)) (filter #(eval-expr (nth sel-expr 1) %) object)))

(defn walk [[opcode operand continuation] object]
  (let [down-obj (cond
         (= opcode :path) (walk-path operand object)
         (= opcode :selector) (walk-selector operand object))]
    (if continuation
      (walk continuation down-obj)
      down-obj)))
