(ns json-path.parser)

(declare parse)

(defn extract-sub-tree [start end stream]
  (take-while #(not (= end %)) (drop-while #(= start %) stream)))

(defn parse-expr [remaining]
  (let [ops {"=" :eq, "!=" :neq, "<" :lt, "<=" :lt-eq, ">" :gt, ">=" :gt-eq}
        supported-ops (set (keys ops))
        lhs (take-while #(not (supported-ops %)) remaining)
        op (first (drop-while #(not (supported-ops %)) remaining))
        rhs (rest (drop-while #(not (supported-ops %)) remaining))]
    (if (nil? op)
      [:some (parse lhs)]
      [(ops op) (parse lhs) (parse rhs)])))

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
  (parse (re-seq #"<=|>=|\.\.|[.*$@\[\]\(\)\"=<>]|\d+|[\w-]+|\?\(|!=" path)))
