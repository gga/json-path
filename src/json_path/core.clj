(ns json-path
  [:use [midje.sweet]])

(unfinished )

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

(defn parse-expr [remaining]
  (let [supported-ops #{"="}
        lhs (take-while #(not (supported-ops %)) remaining)
        op (first (drop-while #(not (supported-ops %)) remaining))
        rhs (rest (drop-while #(not (supported-ops %)) remaining))]
    {:remaining '(), :tree [({"=" :eq} op)
                            (parse {:remaining lhs, :tree []})
                            (parse {:remaining rhs, :tree []})]}))

(fact
  (:tree (parse-expr  '("@" "." "foo" "=" "\"" "baz" "\"")))
  =>
  [:eq [:path [[:current] [:child] [:key "foo"]]] [:val "baz"]]
  (provided
    (parse {:remaining '("@" "." "foo"), :tree []}) => [:path [[:current] [:child] [:key "foo"]]]
    (parse {:remaining '("\"" "baz" "\""), :tree []}) => [:val "baz"]))

(defn- parse-indexer [remaining]
  (let [sub-expr (take-while #(not (= "]" %)) (drop 1 remaining))
        next (first sub-expr)
        trailing (drop 1 (drop-while #(not (= "]" %)) remaining))]
    (cond
     (= next "*") {:remaining trailing, :tree [:index "*"]}
     (= "?(" next) {:remaining trailing,
                    :tree (:tree (parse-expr (take-while #(not (= ")" %)) sub-expr)))}
     :else {:remaining trailing, :tree [:index next]})))

;.;. FAIL at (NO_SOURCE_FILE:1)
;.;.     Expected: [:index "*"]
;.;.       Actual: [:index nil]
;.;. 
;.;. FAIL at (NO_SOURCE_FILE:1)
;.;.     Expected: [:index "3"]
;.;.       Actual: [:index nil]
;.;. 
;.;. FAIL at (NO_SOURCE_FILE:1)
;.;. You claimed the following was needed, but it was never used:
;.;.     (parse-expr (quote ("\"" "bar" "\"" "=" "\"" "bar" "\"")))
;.;. 
;.;. FAIL at (NO_SOURCE_FILE:1)
;.;.     Expected: [:filter [:eq [:val "bar"] [:val "bar"]]]
;.;.       Actual: [:index "\""]
(fact
  (:tree (parse-indexer '("*"))) => [:index "*"]
  (:tree (parse-indexer '("3"))) => [:index "3"]
  (:tree (parse-indexer '("?(" "\"" "bar" "\"" "=" "\"" "bar" "\"" ")"))) => [:filter [:eq [:val "bar"] [:val "bar"]]]
  (provided
    (parse-expr '("\"" "bar" "\"" "=" "\"" "bar" "\"")) => [:eq [:val "bar"] [:val "bar"]]))

(defn- parse [{:keys [remaining tree] :as ctxt}]
  (let [next (first remaining)
        simple-cmd ({"$" :root, "." :child, ".." :all-children, "@" :current} next)]
    (if (empty? remaining)
      tree
      (recur (cond
              (not (nil? simple-cmd)) {:remaining (rest remaining), :tree (conj tree [simple-cmd])}
              (= "[" next) (parse-indexer (assoc ctxt :remaining remaining))
              (= "*" next) {:remaining (rest remaining), :tree (conj tree [:key "*"])}
              :else {:remaining (rest remaining), :tree (conj tree [:key next])})))))

(facts
  (parse {:remaining '("$"), :tree []}) => [:path [:root]]
  (parse {:remaining '("$" "." "*"), :tree []}) => [:path [:root] [:child] [:key "*"]]
  (parse {:remaining '("$" "." "foo" "[" "3" "]"), :tree []}) => [:path [:root] [:child] [:key "foo"] [:index "3"]])

(defn- parse-path [path]
  (parse {:remaining (re-seq #"\.\.|[.*$@\[\]\(\)\"=]|\d+|\w+|\?\(" path),
          :tree []}))

(facts
  (parse-path "") => []
  (parse-path "$") => [[:root]]
  (parse-path "$.hello") => [[:root] [:child] [:key "hello"]]
  (parse-path "$.*") => [[:root] [:child] [:key "*"]]
  (parse-path "$..hello") => [[:root] [:all-children] [:key "hello"]]
  (parse-path "$.foo[3]") => [[:root] [:child] [:key "foo"] [:index "3"]]
  (parse-path "foo[*]") => [[:key "foo"] [:index "*"]]
  (parse-path "$.foo[?(@.bar=\"baz\")].hello") => [[:root] [:child] [:key "foo"]
                                                     [:filter [:eq [:path [:current]
                                                                    [:child]
                                                                    [:key "bar"]]
                                                               [:val "baz"]]]
                                                     [:key "hello"]])

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
