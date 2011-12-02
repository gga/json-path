(ns json-path
  [:use [midje.sweet]])

(defn- eval-expr [[expr-type & operands] object]
  (cond
   (= expr-type :eq) (apply = (map #(eval-expr % object) operands))
   (= expr-type :val) (first operands)
   (= expr-type :path) (eval-path operands object)))

(facts
  (eval-expr [:eq [:val "a"] [:val "b"]] {}) => falsey
  (eval-expr [:eq [:val "a"] [:val "a"]] {}) => truthy
  (eval-expr [:path [:key "foo"]] {:foo "bar"}) => "bar"
  (eval-expr [:eq [:path [:key "foo"]] [:val "bar"]] {:foo "bar"}) => truthy)

(defn- select-by [[opcode & operands :as obj-spec] object]
  (cond
   (sequential? object) (vec (flatten (filter #(not (empty? %))
                                              (map #(select-by obj-spec %) object))))
   :else (cond
          (= (first operands) "*") (vec (vals object))
          :else ((keyword (first operands)) object))))

(facts
  (select-by [:key "hello"] {:hello "world"}) => "world"
  (select-by [:key "hello"] [{:hello "foo"} {:hello "bar"}]) => ["foo" "bar"]
  (select-by [:key "hello"] [{:blah "foo"} {:hello "bar"}]) => [ "bar"]
  (select-by [:key "*"] {:hello "world"}) => ["world"]
  (sort (select-by [:key "*"] {:hello "world", :foo "bar"})) => ["bar", "world"]
  (sort (select-by [:key "*"] [{:hello "world"}, {:foo "bar"}])) => ["bar", "world"])

(defn- extract-sub-tree [start end stream]
  (take-while #(not (= end %)) (drop-while #(= start %) stream)))

(facts
  (extract-sub-tree 4 8 (range 4 10)) => [5 6 7])

(defn parse-expr [remaining]
  (let [supported-ops #{"="}
        lhs (take-while #(not (supported-ops %)) remaining)
        op (first (drop-while #(not (supported-ops %)) remaining))
        rhs (rest (drop-while #(not (supported-ops %)) remaining))]
    [({"=" :eq} op) (parse lhs) (parse rhs)]))

(fact
  (parse-expr  '("@" "." "foo" "=" "\"" "baz" "\""))
  =>
  [:eq [:path [[:current] [:child] [:key "foo"]]] [:val "baz"]]
  (provided
    (parse '("@" "." "foo")) => [:path [[:current] [:child] [:key "foo"]]]
    (parse '("\"" "baz" "\"")) => [:val "baz"]))

(defn- parse-indexer [remaining]
  (let [next (first remaining)]
    (cond
     (= next "*") [:index "*"]
     (= "?(" next) [:filter (parse-expr (extract-sub-tree "?(" ")" (drop 1 remaining)))]
     :else [:index next])))

(fact
  (parse-indexer '("*")) => [:index "*"]
  (parse-indexer '("3")) => [:index "3"]
  (parse-indexer '("?(" "\"" "bar" "\"" "=" "\"" "bar" "\"" ")")) => [:filter [:eq [:val "bar"] [:val "bar"]]])

(defn- parse-path-components [parts]
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
                        [idx (parse rem)]
                        idx)))
     :else (do
             (let [pth (parse-path-components (extract-sub-tree "" "[" remaining))
                   rem (drop-while #(not (= "[" %)) remaining)]
               (if (not (empty? rem))
                 [:path (conj pth (parse rem))]
                 [:path pth]))))))

(facts
  (parse '("\"" "bar" "\"")) => [:val "bar"]
  (parse '("$")) => [:path [[:root]]]
  (parse '("$" "." "*")) => [:path [[:root] [:child] [:key "*"]]]
  (parse '("$" "." "foo" "[" "3" "]")) => [:path [[:root] [:child] [:key "foo"] [:index "3"]]]
  (parse '("[" "3" "]" "." "bar")) => [[:index "3"] [:path [[:child] [:key "bar"]]]])

(defn- parse-path [path]
  (parse (re-seq #"\.\.|[.*$@\[\]\(\)\"=]|\d+|\w+|\?\(" path)))

(facts
  (parse-path "") => []
  (parse-path "$") => [:path [[:root]]]
  (parse-path "$.hello") => [:path [[:root] [:child] [:key "hello"]]]
  (parse-path "$.*") => [:path [[:root] [:child] [:key "*"]]]
  (parse-path "$..hello") => [:path [[:root] [:all-children] [:key "hello"]]]
  (parse-path "$.foo[3]") => [:path [[:root] [:child] [:key "foo"] [:index "3"]]]
  (parse-path "foo[*]") => [:path [[:key "foo"] [:index "*"]]]
  (parse-path "$.foo[?(@.bar=\"baz\")].hello") => [:path [[:root] [:child] [:key "foo"]
                                                          [:filter [:eq [:path [[:current]
                                                                                [:child]
                                                                                [:key "bar"]]]
                                                                    [:val "baz"]]]
                                                          [:path [[:key "hello"]]]]])

(defn- path-filter? [[opcode & operands]]
  (not (empty? (filter #(= opcode %) [:root :current :child :all-children :index :filter]))))

(facts
  (path-filter? [:key "hello"]) => falsey
  (path-filter? [:root]) => truthy
  (path-filter? [:current]) => truthy
  (path-filter? [:child]) => truthy
  (path-filter? [:all-children]) => truthy
  (path-filter? [:index "3"]) => truthy
  (path-filter? [:filter :expression]) => truthy)

(defn- walk [[opcode & operands] object]
  (let [obj-aggregator (fn obj-aggregator [obj]
                         (let [obj-vals (vec (filter map? (vals obj)))
                               children (flatten (map obj-aggregator obj-vals))]
                           (vec (concat obj-vals children))))]
    (cond
     (or (= opcode :root) (= opcode :child) (= opcode :current)) object
     (= opcode :all-children) (vec (concat [object] (obj-aggregator object)))
     (= opcode :index) (let [selector (first operands)]
                           (cond
                            (= selector "*") object
                            :else (nth object (Integer/parseInt (first operands)))))
     (= opcode :filter) (filter #(eval-expr (first operands) %) object))))

(facts
  (walk [:root] ...json...) => ...json...
  (walk [:child] ...json...) => ...json...
  (walk [:current] ...json...) => ...json...
  (walk [:all-children] {:hello {:world "foo"},
                         :baz {:world "bar",
                               :quuz {:world "zux"}}}) => [{:hello {:world "foo"}
                                                            :baz {:world "bar", :quuz {:world "zux"}}},
                                                           {:world "foo"},
                                                           {:world "bar",
                                                            :quuz {:world "zux"}},
                                                           {:world "zux"}]
  (walk [:index "1"] ["foo", "bar", "baz"]) => "bar"
  (walk [:index "*"] ["foo", "bar", "baz"]) => ["foo", "bar", "baz"]
  (walk [:filter [:eq
                  [:path [:current]
                   [:child]
                   [:key "bar"]]
                  [:val "baz"]]] [{:bar "wrong"} {:bar "baz"}]) => [{:bar "baz"}])

(defn- eval-path [path object]
  (loop [parts path
         target object]
    (if (empty? parts)
      target
      (let [next (first parts)]
        (cond
         (path-filter? next) (recur (rest parts)
                                    (walk next target))
         :else (recur (rest parts)
                      (select-by next target)))))))

(defn at-path [path object]
  (eval-path (parse-path path) object))

(facts
  (at-path "$" ...json...) => ...json...
  (at-path "$.hello" {:hello "world"}) => "world"
  (at-path "$.hello.world" {:hello {:world "foo"}}) => "foo"
  (at-path "$..world" {:hello {:world "foo"},
                       :baz   {:world "bar",
                               :quuz {:world "zux"}}}) => ["foo", "bar", "zux"]
  (at-path "$.*.world" {:a {:world "foo"},
                        :b {:world "bar",
                            :c {:world "baz"}}}) => ["foo", "bar"]
  (at-path "$.foo[*]" {:foo ["a", "b", "c"]}) => ["a", "b", "c"]
  (at-path "$.foo[?(@.bar=\"baz\")].hello"
           {:foo [{:bar "wrong" :hello "goodbye"}
                  {:bar "baz" :hello "world"}]}) => ["world"]
  (provided
    (parse-path "$.foo[?(@.bar=\"baz\")].hello") => [[:root] [:child] [:key "foo"]
                                                     [:filter [:eq [:path [:current]
                                                                    [:child]
                                                                    [:key "bar"]]
                                                               [:val "baz"]]]
                                                     [:key "hello"]]))
