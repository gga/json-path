(ns json-path.parser)

(declare parse parse-expr)

(defn extract-sub-tree [start end stream]
  (take-while #(not (= end %)) (drop-while #(= start %) stream)))

(def boolean-ops
  {"&&" :and, "||" :or})

(def comparator-ops
  {"=" :eq, "!=" :neq, "<" :lt, "<=" :lt-eq, ">" :gt, ">=" :gt-eq})

(def boolean-ops-strings (set (keys boolean-ops)))
(def comparator-ops-strings (set (keys comparator-ops)))

(defn parse-boolean-expr [expr]
  (let [lhs (take-while #(not (boolean-ops-strings %)) expr)
        op (first (drop-while #(not (boolean-ops-strings %)) expr))
        rhs (rest (drop-while #(not (boolean-ops-strings %)) expr))]
    [(boolean-ops op) (parse-expr lhs) (parse-expr rhs)]))

(defn parse-comparator-expr [expr]
  (let [lhs (take-while #(not (comparator-ops-strings %)) expr)
        op (first (drop-while #(not (comparator-ops-strings %)) expr))
        rhs (rest (drop-while #(not (comparator-ops-strings %)) expr))]
    [(comparator-ops op) (parse lhs) (parse rhs)]))

(defn parse-expr [expr]
  (cond
    (some boolean-ops-strings expr) (parse-boolean-expr expr)
    (some (set (keys comparator-ops)) expr) (parse-comparator-expr expr)
    :else [:bool (parse expr)]))

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
     (re-matches #"\d+" next) [:val (Integer/parseInt next)]
     (re-matches #"\d+\.\d*" next) [:val (Double/parseDouble next)]
     (= next "true") [:val true]
     (= next "false") [:val false]
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
  (parse (re-seq #"<=|>=|\.\.|[.*$@\[\]\(\)\"=<>]|\d+|[\w-\/]+|\?\(|!=|&&|\|\||true|false" path)))
