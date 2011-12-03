(ns json-path)

(declare parse)

(defn extract-sub-tree [start end stream]
  (take-while #(not (= end %)) (drop-while #(= start %) stream)))

(defn parse-expr [remaining]
  (let [supported-ops #{"="}
        lhs (take-while #(not (supported-ops %)) remaining)
        op (first (drop-while #(not (supported-ops %)) remaining))
        rhs (rest (drop-while #(not (supported-ops %)) remaining))]
    [({"=" :eq} op) (parse lhs) (parse rhs)]))

(defn parse-indexer [remaining]
  (let [next (first remaining)]
    (cond
     (= next "*") [:index "*"]
     (= "?(" next) [:filter (parse-expr (extract-sub-tree "?(" ")" (drop 1 remaining)))]
     :else [:index next])))

(defn parse-path-components [parts]
  (let [converter (fn [part]
                    (let [path-cmds {"$" :root, "." :child, ".." :all-children, "@" :current}]
                      (if (path-cmds part)
                        [(path-cmds part)]
                        [:key part])))]
    (vec (map converter parts))))

(defn parse [remaining]
  (let [next (first remaining)]
    (cond
     (empty? remaining) []
     (= "\"" next) [:val (apply str (extract-sub-tree "\"" "\"" remaining))]
     (= "[" next) (do
                    (let [idx (parse-indexer (extract-sub-tree "[" "]" remaining))
                          rem (drop 1 (drop-while #(not (= "]" %)) remaining))]
                      (if (not (empty? rem))
                        [:selector idx (parse rem)]
                        [:selector idx])))
     :else (do
             (let [pth (parse-path-components (extract-sub-tree "" "[" remaining))
                   rem (drop-while #(not (= "[" %)) remaining)]
               (if (not (empty? rem))
                 [:path pth (parse rem)]
                 [:path pth]))))))

(defn parse-path [path]
  (parse (re-seq #"\.\.|[.*$@\[\]\(\)\"=]|\d+|\w+|\?\(" path)))

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

(defn at-path [path object]
  (walk (parse-path path) object))
