(ns json-path.test.walker-test
  [:require [json-path.match :as m]]
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
  (eval-expr [:path [[:key "foo"]]] {:current (m/match {:foo "bar"})}) => "bar"
  (eval-expr [:eq [:path [[:key "foo"]]] [:val "bar"]] {:current (m/match {:foo "bar"})}) => truthy)

(facts
  (select-by [:key "hello"] (m/match {:hello "world"})) => ["world" [:hello]]
  (select-by [:key "hello"] (m/match [{:hello "foo"} {:hello "bar"}])) => '(["foo" [0 :hello]] ["bar" [1 :hello]])
  (select-by [:key "hello"] (m/match [{:blah "foo"} {:hello "bar"}])) => '(["bar" [1 :hello]])
  (select-by [:key "*"] (m/match {:hello "world"})) => '(["world" [:hello]])
  (sort-by first (select-by [:key "*"] (m/match {:hello "world" :foo "bar"}))) => '(["bar" [:foo]] ["world" [:hello]])
  (sort-by first (select-by [:key "*"] (m/match [{:hello "world"} {:foo "bar"}]))) => '(["bar" [1 :foo]] ["world" [0 :hello]]))

(fact
  (walk-path [[:root]] {:root (m/match ...root...), :current  (m/match ...obj...)}) => [...root... []]
  (walk-path [[:root] [:child] [:key "foo"]] {:root (m/match {:foo "bar"})}) => ["bar" [:foo]]
  (walk-path [[:key "foo"]] {:current (m/match [{:foo "bar"} {:foo "baz"} {:foo "qux"}])}) => '(["bar" [0 :foo]] ["baz" [1 :foo]] ["qux" [2 :foo]])
  (walk-path [[:all-children]] {:current (m/match {:foo "bar" :baz {:qux "zoo"}})}) => '([{:foo "bar" :baz {:qux "zoo"}} []]
                                                                                         [{:qux "zoo"} [:baz]])
  (distinct (walk-path [[:all-children] [:key "bar"]] ;; distinct works around dups, mentioned in https://github.com/gga/json-path/pull/6
                       {:current (m/match '([{:bar "hello"}]))})) => '(["hello" [0 0 :bar]])
  (walk-path [[:all-children] [:key "bar"]]
             {:current (m/match {:foo [{:bar "wrong"}
                                       {:bar "baz"}]})}) => '(["wrong" [:foo 0 :bar]]
                                                              ["baz" [:foo 1 :bar]])
  (walk-path [[:all-children] [:key "foo"]]
             {:current (m/match {:foo [{:foo "foo"}]})}) => '([[{:foo "foo"}] [:foo]]
                                                    ["foo" [:foo 0 :foo]]))

(fact
  (walk-selector [:index "1"] {:current (m/match ["foo", "bar", "baz"])}) => ["bar" [1]]
  (walk-selector [:index "*"] {:current (m/match [:a :b])}) => '([:a [0]] [:b [1]])
  (walk-selector [:filter [:eq [:path [[:current] [:child] [:key "bar"]]] [:val "baz"]]]
                 {:current (m/match [{:bar "wrong"} {:bar "baz"}])}) => '([{:bar "baz"} [1]]))

(fact "selecting places constraints on the shape of the object being selected from"
  (walk-selector [:index "1"] {:current (m/match {:foo "bar"})}) => (throws Exception)
  (walk-selector [:index "*"] {:current (m/match {:foo "bar"})}) => (throws Exception))

(facts
  (walk [:path [[:root]]] {:root (m/match ...json...)}) => [...json... []]
  (walk [:path [[:child]]] {:current (m/match ...json...)}) => [...json... []]
  (walk [:path [[:current]]] {:current (m/match ...json...)}) => [...json... []]
  (walk [:path [[:key "foo"]]] {:current (m/match {:foo "bar"})}) => ["bar" [:foo]]
  (walk [:path [[:all-children]]]
        {:current
         (m/match {:hello {:world "foo"},
                   :baz {:world "bar",
                         :quuz {:world "zux"}}})}) => '([{:hello {:world "foo"},
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
         (m/match (list {:hello {:world "foo"}}
                        {:baz {:world "bar"}}))}) => '([[{:hello {:world "foo"}}
                                                         {:baz {:world "bar"}}]
                                                        []]
                                                       [{:hello {:world "foo"}} [0]]
                                                       [{:world "foo"} [0 :hello]]
                                                       [{:baz {:world "bar"}} [1]]
                                                       [{:world "bar"} [1 :baz]])
  (walk [:path [[:all-children]]]
        {:current (m/match "scalar")}) => '(["scalar" []])
  (walk [:path [[:all-children] [:key "world"]]]
        {:current (m/match {:hello {:world "foo"},
                            :baz   {:world "bar",
                                    :quuz {:world "zux"}}})}) => '(["foo" [:hello :world]]
                                                                   ["bar" [:baz :world]]
                                                                   ["zux" [:baz :quuz :world]])
  (walk [:selector [:index "1"]] {:current (m/match ["foo", "bar", "baz"])}) => ["bar" [1]]
  (walk [:selector [:index "*"]] {:current (m/match [:a :b])}) => '([:a [0]] [:b [1]])
  (walk [:selector [:index "*"]
         [:path [[:child] [:key "foo"]]]]
        {:current
         (m/match [{:foo 1} {:foo 2}])}) => '([1 [0 :foo]] [2 [1 :foo]])
  (walk [:selector [:filter [:eq
                             [:path [[:current]
                                     [:child]
                                     [:key "bar"]]]
                             [:val "baz"]]]]
        {:current (m/match [{:bar "wrong"} {:bar "baz"}])}) => '([{:bar "baz"} [1]])
  (walk [:path [[:root] [:child] [:key "foo"]]
         [:selector [:filter [:eq [:path [[:current]
                                          [:child]
                                          [:key "bar"]]]
                              [:val "baz"]]]
          [:path [[:child] [:key "hello"]]]]]
        {:root (m/match {:foo [{:bar "wrong" :hello "goodbye"}
                               {:bar "baz" :hello "world"}]})}) => '(["world" [:foo 1 :hello]]))

(facts "walking a nil object should be safe"
  (walk [:path [[:root]]] {:root (m/match nil)}) => [nil []]
  (walk [:path [[:root] [:child] [:key "foo"]]] {:root (m/match {:bar "baz"})}) => [nil [:foo]]
  (walk [:path [[:root] [:child] [:key "foo"] [:child] [:key "bar"]]]
           {:root (m/match {:foo {:baz "hello"}})}) => [nil [:foo :bar]])
