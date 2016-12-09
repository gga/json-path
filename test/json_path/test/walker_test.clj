(ns json-path.test.walker-test
  [:use [json-path.walker]
   [midje.sweet]])

(unfinished)

(facts
  (eval-expr [:eq [:val "a"] [:val "b"]] {}) => falsey
  (eval-expr [:eq [:val "a"] [:val "a"]] {}) => truthy
  (eval-expr [:neq [:val "a"] [:val "b"]] {}) => truthy
  (eval-expr [:lt [:val 10] [:val 11]] {}) => truthy
  (eval-expr [:lt-eq [:val 10] [:val 10]] {}) => truthy
  (eval-expr [:gt [:val 10] [:val 9]] {}) => truthy
  (eval-expr [:gt-eq [:val 10] [:val 10]] {}) => truthy
  (eval-expr [:path [[:key "foo"]]] {:current {:foo "bar"}}) => "bar"
  (eval-expr [:eq [:path [[:key "foo"]]] [:val "bar"]] {:current {:foo "bar"}}) => truthy)

(facts
  (select-by [:key "hello"] {:current {:hello "world"}}) => ["world" [:hello]]
  (select-by [:key "hello"] {:current [{:hello "foo"} {:hello "bar"}]}) => '(["foo" [0 :hello]] ["bar" [1 :hello]])
  (select-by [:key "hello"] {:current [{:blah "foo"} {:hello "bar"}]}) => '(["bar" [1 :hello]])
  (select-by [:key "*"] {:current {:hello "world"}}) => '(["world" [:hello]])
  (sort-by first (select-by [:key "*"] {:current {:hello "world" :foo "bar"}})) => '(["bar" [:foo]] ["world" [:hello]])
  (sort-by first (select-by [:key "*"] {:current [{:hello "world"} {:foo "bar"}]})) => '(["bar" [1 :foo]] ["world" [0 :hello]]))

(fact
  (walk-path [[:root]] {:root ...root..., :current  ...obj...}) => [...root... []]
  (walk-path [[:root] [:child] [:key "foo"]] {:root {:foo "bar"}}) => ["bar" [:foo]]
  (walk-path [[:key "foo"]] {:current [{:foo "bar"} {:foo "baz"} {:foo "qux"}]}) => '(["bar" [0 :foo]] ["baz" [1 :foo]] ["qux" [2 :foo]])
  (walk-path [[:all-children]] {:current {:foo "bar" :baz {:qux "zoo"}}}) => '([{:foo "bar" :baz {:qux "zoo"}} []]
                                                                               [{:qux "zoo"} [:baz]])
  (distinct (walk-path [[:all-children] [:key "bar"]] ;; distinct works around dups, mentioned in https://github.com/gga/json-path/pull/6
                       {:current '([{:bar "hello"}])})) => '(["hello" [0 0 :bar]]))

(fact
  (walk-selector [:index "1"] {:current ["foo", "bar", "baz"]}) => ["bar" [1]]
  (walk-selector [:index "*"] {:current [:a :b]}) => '([:a [0]] [:b [1]])
  (walk-selector [:filter [:eq [:path [[:current] [:child] [:key "bar"]]] [:val "baz"]]]
                 {:current  [{:bar "wrong"} {:bar "baz"}]}) => '([{:bar "baz"} [1]]))

(fact "selecting places constraints on the shape of the object being selected from"
  (walk-selector [:index "1"] {:current {:foo "bar"}}) => (throws Exception)
  (walk-selector [:index "*"] {:current {:foo "bar"}}) => (throws Exception))

(facts
  (walk [:path [[:root]]] {:root ...json...}) => [...json... []]
  (walk [:path [[:child]]] {:current ...json...}) => [...json... []]
  (walk [:path [[:current]]] {:current ...json...}) => [...json... []]
  (walk [:path [[:key "foo"]]] {:current {:foo "bar"}}) => ["bar" [:foo]]
  (walk [:path [[:all-children]]]
        {:current
         {:hello {:world "foo"},
          :baz {:world "bar",
                :quuz {:world "zux"}}}}) => '([{:hello {:world "foo"},
                                                :baz {:world "bar", :quuz {:world "zux"}}}
                                               []]
                                              [{:world "foo"}
                                               [:hello]]
                                              [{:world "bar",
                                                :quuz {:world "zux"}}
                                               [:baz]]
                                              [{:world "zux"}
                                               [:baz :quuz]])
  (walk [:path [[:all-children]]]
        {:current
         (list {:hello {:world "foo"}}
               {:baz {:world "bar"}})}) => '([[{:hello {:world "foo"}}
                                               {:baz {:world "bar"}}]
                                              []]
                                             [{:hello {:world "foo"}} [0]]
                                             [{:baz {:world "bar"}} [1]]
                                             [{:world "foo"} [0 :hello]]
                                             [{:world "bar"} [1 :baz]])
  (walk [:path [[:all-children]]]
        {:current "scalar"}) => '(["scalar" []])
  (walk [:path [[:all-children] [:key "world"]]]
        {:current {:hello {:world "foo"},
                   :baz   {:world "bar",
                           :quuz {:world "zux"}}}}) => '(["foo" [:hello :world]]
                                                         ["bar" [:baz :world]]
                                                         ["zux" [:baz :quuz :world]])
  (walk [:selector [:index "1"]] {:current ["foo", "bar", "baz"]}) => ["bar" [1]]
  (walk [:selector [:index "*"]] {:current [:a :b]}) => '([:a [0]] [:b [1]])
  (walk [:selector [:index "*"]
         [:path [[:child] [:key "foo"]]]]
        {:current
         [{:foo 1} {:foo 2}]}) => '([1 [0 :foo]] [2 [1 :foo]])
  (walk [:selector [:filter [:eq
                             [:path [[:current]
                                     [:child]
                                     [:key "bar"]]]
                             [:val "baz"]]]]
        {:current [{:bar "wrong"} {:bar "baz"}]}) => '([{:bar "baz"} [1]])
  (walk [:path [[:root] [:child] [:key "foo"]]
         [:selector [:filter [:eq [:path [[:current]
                                          [:child]
                                          [:key "bar"]]]
                              [:val "baz"]]]
          [:path [[:child] [:key "hello"]]]]]
        {:root {:foo [{:bar "wrong" :hello "goodbye"}
                      {:bar "baz" :hello "world"}]}}) => '(["world" [:foo 1 :hello]]))

(facts "walking a nil object should be safe"
  (walk [:path [[:root]]] nil) => [nil []]
  (walk [:path [[:root] [:child] [:key "foo"]]] {:bar "baz"}) => [nil [:foo]]
  (walk [:path [[:root] [:child] [:key "foo"] [:child] [:key "bar"]]]
           {:foo {:baz "hello"}}) => [nil [:foo :bar]])
