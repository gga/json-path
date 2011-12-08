(ns json-path.test.walker-test
  [:use [json-path.walker]
   [midje.sweet]])

(unfinished)

(facts
  (eval-expr [:eq [:val "a"] [:val "b"]] {}) => falsey
  (eval-expr [:eq [:val "a"] [:val "a"]] {}) => truthy
  (eval-expr [:path [[:key "foo"]]] {:current {:foo "bar"}}) => "bar"
  (eval-expr [:eq [:path [[:key "foo"]]] [:val "bar"]] {:current {:foo "bar"}}) => truthy)

(facts
  (select-by [:key "hello"] {:current {:hello "world"}}) => "world"
  (select-by [:key "hello"] {:current [{:hello "foo"} {:hello "bar"}]}) => ["foo" "bar"]
  (select-by [:key "hello"] {:current [{:blah "foo"} {:hello "bar"}]}) => [ "bar"]
  (select-by [:key "*"] {:current {:hello "world"}}) => ["world"]
  (sort (select-by [:key "*"] {:current {:hello "world", :foo "bar"}})) => ["bar", "world"]
  (sort (select-by [:key "*"] {:current [{:hello "world"}, {:foo "bar"}]})) => ["bar", "world"])

(fact
  (walk-path [[:root]] {:root ...root..., :current  ...obj...}) => ...root...
  (walk-path [[:root] [:child] [:key "foo"]] {:root {:foo "bar"}}) => "bar"
  (walk-path [[:all-children]] {:current {:foo "bar" :baz {:qux "zoo"}}}) => [{:foo "bar" :baz {:qux "zoo"}},
                                                                   {:qux "zoo"}])

(fact
  (walk-selector [:index "1"] {:current  ["foo", "bar", "baz"]}) => "bar"
  (walk-selector [:index "*"] {:current ...array...}) => ...array...
  (walk-selector [:filter [:eq [:path [[:current] [:child] [:key "bar"]]] [:val "baz"]]]
                 {:current  [{:bar "wrong"} {:bar "baz"}]}) => [{:bar "baz"}])

(facts
  (walk [:path [[:root]]] {:current ...json...}) => ...json...
  (walk [:path [[:child]]] {:current ...json...}) => ...json...
  (walk [:path [[:current]]] {:current ...json...}) => ...json...
  (walk [:path [[:key "foo"]]] {:current {:foo "bar"}}) => "bar"
  (walk [:path [[:all-children]]]
        {:current
         {:hello {:world "foo"},
          :baz {:world "bar",
                :quuz {:world "zux"}}}}) => [{:hello {:world "foo"},
                                              :baz {:world "bar", :quuz {:world "zux"}}},
                                             {:world "foo"},
                                             {:world "bar",
                                              :quuz {:world "zux"}},
                                             {:world "zux"}]
  (walk [:selector [:index "1"]] {:current ["foo", "bar", "baz"]}) => "bar"
  (walk [:selector [:index "*"]] {:current ...array...}) => ...array...
  (walk [:selector [:filter [:eq
                             [:path [[:current]
                                     [:child]
                                     [:key "bar"]]]
                             [:val "baz"]]]]
        {:current [{:bar "wrong"} {:bar "baz"}]}) => [{:bar "baz"}]
  (walk [:path [[:root] [:child] [:key "foo"]]
         [:selector [:filter [:eq [:path [[:current]
                                          [:child]
                                          [:key "bar"]]]
                              [:val "baz"]]]
          [:path [[:child] [:key "hello"]]]]]
        {:current {:foo [{:bar "wrong" :hello "goodbye"}
                         {:bar "baz" :hello "world"}]}}) => ["world"])
