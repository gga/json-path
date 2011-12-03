(ns json-path
  [:use [midje.sweet]])

(unfinished )

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
  [:eq [:path [[:current] [:child] [:key "foo"]]] [:val "baz"]])

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
                        [:selector idx (parse rem)]
                        [:selector idx])))
     :else (do
             (let [pth (parse-path-components (extract-sub-tree "" "[" remaining))
                   rem (drop-while #(not (= "[" %)) remaining)]
               (if (not (empty? rem))
                 [:path pth (parse rem)]
                 [:path pth]))))))

(facts
  (parse '("\"" "bar" "\"")) => [:val "bar"]
  (parse '("$")) => [:path [[:root]]]
  (parse '("$" "." "*")) => [:path [[:root] [:child] [:key "*"]]]
  (parse '("$" "." "foo" "[" "3" "]")) => [:path [[:root] [:child] [:key "foo"]] [:selector [:index "3"]]]
  (parse '("$" "[" "3" "]" "." "bar")) => [:path [[:root]] [:selector [:index "3"] [:path [[:child] [:key "bar"]]]]])

(defn- parse-path [path]
  (parse (re-seq #"\.\.|[.*$@\[\]\(\)\"=]|\d+|\w+|\?\(" path)))

(facts
  (parse-path "") => []
  (parse-path "$") => [:path [[:root]]]
  (parse-path "$.hello") => [:path [[:root] [:child] [:key "hello"]]]
  (parse-path "$.*") => [:path [[:root] [:child] [:key "*"]]]
  (parse-path "$..hello") => [:path [[:root] [:all-children] [:key "hello"]]]
  (parse-path "$.foo[3]") => [:path [[:root] [:child] [:key "foo"]] [:selector [:index "3"]]]
  (parse-path "foo[*]") => [:path [[:key "foo"]] [:selector [:index "*"]]]
  (parse-path "$.foo[?(@.bar=\"baz\")].hello") => [:path [[:root] [:child] [:key "foo"]]
                                                   [:selector [:filter [:eq [:path [[:current]
                                                                                    [:child]
                                                                                    [:key "bar"]]]
                                                                        [:val "baz"]]]
                                                    [:path [[:child] [:key "hello"]]]]])

(defn- eval-expr [[expr-type & operands :as expr] object]
  (cond
   (= expr-type :eq) (apply = (map #(eval-expr % object) operands))
   (= expr-type :val) (first operands)
   (= expr-type :path) (walk expr object)))

(facts
  (eval-expr [:eq [:val "a"] [:val "b"]] {}) => falsey
  (eval-expr [:eq [:val "a"] [:val "a"]] {}) => truthy
  (eval-expr [:path [[:key "foo"]]] {:foo "bar"}) => "bar"
  (eval-expr [:eq [:path [[:key "foo"]]] [:val "bar"]] {:foo "bar"}) => truthy)

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

(fact
  (walk-path [[:root]] ...obj...) => ...obj...
  (walk-path [[:root] [:child] [:key "foo"]] {:foo "bar"}) => "bar"
  (walk-path [[:all-children]] {:foo "bar" :baz {:qux "zoo"}}) => [{:foo "bar" :baz {:qux "zoo"}},
                                                                   {:qux "zoo"}])

(defn walk-selector [sel-expr object]
  (cond
   (= :index (first sel-expr)) (let [sel (nth sel-expr 1)]
                                 (if (= "*" sel)
                                   object
                                   (nth object (Integer/parseInt sel))))
   (= :filter (first sel-expr)) (filter #(eval-expr (nth sel-expr 1) %) object))))

(fact
  (walk-selector [:index "1"] ["foo", "bar", "baz"]) => "bar"
  (walk-selector [:index "*"] ...array...) => ...array...
  (walk-selector [:filter [:eq [:path [[:current] [:child] [:key "bar"]]] [:val "baz"]]]
                 [{:bar "wrong"} {:bar "baz"}]) => [{:bar "baz"}])

(defn- walk [[opcode operand continuation] object]
  (let [down-obj (cond
         (= opcode :path) (walk-path operand object)
         (= opcode :selector) (walk-selector operand object))]
    (if continuation
      (walk continuation down-obj)
      down-obj)))

(facts
  (walk [:path [[:root]]] ...json...) => ...json...
  (walk [:path [[:child]]] ...json...) => ...json...
  (walk [:path [[:current]]] ...json...) => ...json...
  (walk [:path [[:key "foo"]]] {:foo "bar"}) => "bar"
  (walk [:path [[:all-children]]]
        {:hello {:world "foo"},
         :baz {:world "bar",
               :quuz {:world "zux"}}}) => [{:hello {:world "foo"},
         :baz {:world "bar", :quuz {:world "zux"}}},
        {:world "foo"},
        {:world "bar",
         :quuz {:world "zux"}},
        {:world "zux"}]
  (walk [:selector [:index "1"]] ["foo", "bar", "baz"]) => "bar"
  (walk [:selector [:index "*"]] ...array...) => ...array...
  (walk [:selector [:filter [:eq
                             [:path [[:current]
                                     [:child]
                                     [:key "bar"]]]
                             [:val "baz"]]]]
        [{:bar "wrong"} {:bar "baz"}]) => [{:bar "baz"}]
  (walk [:path [[:root] [:child] [:key "foo"]]
         [:selector [:filter [:eq [:path [[:current]
                                          [:child]
                                          [:key "bar"]]]
                              [:val "baz"]]]
          [:path [[:child] [:key "hello"]]]]]
        {:foo [{:bar "wrong" :hello "goodbye"}
               {:bar "baz" :hello "world"}]})) => ["world"]

(defn at-path [path object]
  (walk (parse-path path) object))

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
                  {:bar "baz" :hello "world"}]}) => ["world"])
