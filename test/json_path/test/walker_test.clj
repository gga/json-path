(ns json-path.test.walker-test
  [:use [json-path.walker]
   [midje.sweet]])

(unfinished)

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
