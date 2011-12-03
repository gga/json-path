(ns json-path.test.json-path-test
  [:use [json-path]
   [midje.sweet]])

(unfinished)

(facts
  (extract-sub-tree 4 8 (range 4 10)) => [5 6 7])

(fact
  (parse-expr  '("@" "." "foo" "=" "\"" "baz" "\""))
  =>
  [:eq [:path [[:current] [:child] [:key "foo"]]] [:val "baz"]])

(fact
  (parse-indexer '("*")) => [:index "*"]
  (parse-indexer '("3")) => [:index "3"]
  (parse-indexer '("?(" "\"" "bar" "\"" "=" "\"" "bar" "\"" ")")) => [:filter [:eq [:val "bar"] [:val "bar"]]])

(facts
  (parse '("\"" "bar" "\"")) => [:val "bar"]
  (parse '("$")) => [:path [[:root]]]
  (parse '("$" "." "*")) => [:path [[:root] [:child] [:key "*"]]]
  (parse '("$" "." "foo" "[" "3" "]")) => [:path [[:root] [:child] [:key "foo"]] [:selector [:index "3"]]]
  (parse '("$" "[" "3" "]" "." "bar")) => [:path [[:root]] [:selector [:index "3"] [:path [[:child] [:key "bar"]]]]])

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

(facts
  (eval-expr [:eq [:val "a"] [:val "b"]] {}) => falsey
  (eval-expr [:eq [:val "a"] [:val "a"]] {}) => truthy
  (eval-expr [:path [[:key "foo"]]] {:foo "bar"}) => "bar"
  (eval-expr [:eq [:path [[:key "foo"]]] [:val "bar"]] {:foo "bar"}) => truthy)

(facts
  (select-by [:key "hello"] {:hello "world"}) => "world"
  (select-by [:key "hello"] [{:hello "foo"} {:hello "bar"}]) => ["foo" "bar"]
  (select-by [:key "hello"] [{:blah "foo"} {:hello "bar"}]) => [ "bar"]
  (select-by [:key "*"] {:hello "world"}) => ["world"]
  (sort (select-by [:key "*"] {:hello "world", :foo "bar"})) => ["bar", "world"]
  (sort (select-by [:key "*"] [{:hello "world"}, {:foo "bar"}])) => ["bar", "world"])

(fact
  (walk-path [[:root]] ...obj...) => ...obj...
  (walk-path [[:root] [:child] [:key "foo"]] {:foo "bar"}) => "bar"
  (walk-path [[:all-children]] {:foo "bar" :baz {:qux "zoo"}}) => [{:foo "bar" :baz {:qux "zoo"}},
                                                                   {:qux "zoo"}])

(fact
  (walk-selector [:index "1"] ["foo", "bar", "baz"]) => "bar"
  (walk-selector [:index "*"] ...array...) => ...array...
  (walk-selector [:filter [:eq [:path [[:current] [:child] [:key "bar"]]] [:val "baz"]]]
                 [{:bar "wrong"} {:bar "baz"}]) => [{:bar "baz"}])

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
