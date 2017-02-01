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
  (select-by [:key "hello"] (m/match {:hello "world"})) => (m/create-match "world" [:hello])
  (select-by [:key "hello"] (m/match [{:hello "foo"} {:hello "bar"}])) => (list (m/create-match "foo" [0 :hello]) (m/create-match "bar" [1 :hello]))
  (select-by [:key "hello"] (m/match [{:blah "foo"} {:hello "bar"}])) => (list (m/create-match "bar" [1 :hello]))
  (select-by [:key "*"] (m/match {:hello "world"})) => (list (m/create-match "world" [:hello]))
  (sort-by first (select-by [:key "*"] (m/match {:hello "world" :foo "bar"}))) => (list (m/create-match "bar" [:foo]) (m/create-match "world" [:hello]))
  (sort-by first (select-by [:key "*"] (m/match [{:hello "world"} {:foo "bar"}]))) => (list (m/create-match "world" [0 :hello]) (m/create-match "bar" [1 :foo])))

(facts
  (walk-path [[:root]] {:root (m/match ...root...), :current  (m/match ...obj...)}) => (m/create-match ...root... [])
  (walk-path [[:root] [:child] [:key "foo"]] {:root (m/match {:foo "bar"})}) => (m/create-match "bar" [:foo])
  (walk-path [[:key "foo"]] {:current (m/match [{:foo "bar"} {:foo "baz"} {:foo "qux"}])}) => (list (m/create-match "bar" [0 :foo]) (m/create-match "baz" [1 :foo]) (m/create-match "qux" [2 :foo]))
  (walk-path [[:all-children]] {:current (m/match {:foo "bar" :baz {:qux "zoo"}})}) => (list (m/create-match {:foo "bar" :baz {:qux "zoo"}} [])
                                                                                             (m/create-match {:qux "zoo"} [:baz]))
  (distinct (walk-path [[:all-children] [:key "bar"]] ;; distinct works around dups, mentioned in https://github.com/gga/json-path/pull/6
                       {:current (m/match '([{:bar "hello"}]))})) => (list (m/create-match "hello" [0 0 :bar]))
  (walk-path [[:all-children] [:key "bar"]]
             {:current (m/match {:foo [{:bar "wrong"}
                                       {:bar "baz"}]})}) => (list (m/create-match "wrong" [:foo 0 :bar])
                                                                  (m/create-match "baz" [:foo 1 :bar]))
  (walk-path [[:all-children] [:key "foo"]]
             {:current (m/match {:foo [{:foo "foo"}]})}) => (list (m/create-match [{:foo "foo"}] [:foo])
                                                                  (m/create-match "foo" [:foo 0 :foo])))

(facts
  (walk-selector [:index "1"] {:current (m/match ["foo", "bar", "baz"])}) => (m/create-match "bar" [1])
  (walk-selector [:index "*"] {:current (m/match [:a :b])}) => (list (m/create-match :a [0]) (m/create-match :b [1]))
  (walk-selector [:filter [:eq [:path [[:current] [:child] [:key "bar"]]] [:val "baz"]]]
                 {:current (m/match [{:bar "wrong"} {:bar "baz"}])}) => (list (m/create-match {:bar "baz"} [1])))

(fact "selecting places constraints on the shape of the object being selected from"
  (walk-selector [:index "1"] {:current (m/match {:foo "bar"})}) => (throws Exception)
  (walk-selector [:index "*"] {:current (m/match {:foo "bar"})}) => (throws Exception))

(facts
  (walk [:path [[:root]]] {:root (m/match ...json...)}) => (m/create-match ...json... [])
  (walk [:path [[:child]]] {:current (m/match ...json...)}) => (m/create-match ...json... [])
  (walk [:path [[:current]]] {:current (m/match ...json...)}) => (m/create-match ...json... [])
  (walk [:path [[:key "foo"]]] {:current (m/match {:foo "bar"})}) => (m/create-match "bar" [:foo])
  (walk [:path [[:all-children]]]
        {:current
         (m/match {:hello {:world "foo"},
                   :baz {:world "bar",
                         :quuz {:world "zux"}}})}) => (list (m/create-match {:hello {:world "foo"},
                                                                             :baz {:world "bar", :quuz {:world "zux"}}}
                                                                            [])
                                                            (m/create-match {:world "foo"}
                                                                            [:hello])
                                                            (m/create-match {:world "bar",
                                                                             :quuz {:world "zux"}}
                                                                            [:baz])
                                                            (m/create-match {:world "zux"}
                                                                            [:baz :quuz]))
  (walk [:path [[:all-children]]]
        {:current
         (m/match (list {:hello {:world "foo"}}
                        {:baz {:world "bar"}}))}) => (list (m/create-match [{:hello {:world "foo"}}
                                                                            {:baz {:world "bar"}}]
                                                                           [])
                                                           (m/create-match {:hello {:world "foo"}} [0])
                                                           (m/create-match {:world "foo"} [0 :hello])
                                                           (m/create-match {:baz {:world "bar"}} [1])
                                                           (m/create-match {:world "bar"} [1 :baz]))
  (walk [:path [[:all-children]]]
        {:current (m/match "scalar")}) => (list (m/create-match "scalar" []))
  (walk [:path [[:all-children] [:key "world"]]]
        {:current (m/match {:hello {:world "foo"},
                            :baz   {:world "bar",
                                    :quuz {:world "zux"}}})}) => (list (m/create-match "foo" [:hello :world])
                                                                       (m/create-match "bar" [:baz :world])
                                                                       (m/create-match "zux" [:baz :quuz :world]))
  (walk [:selector [:index "1"]] {:current (m/match ["foo", "bar", "baz"])}) => (m/create-match "bar" [1])
  (walk [:selector [:index "*"]] {:current (m/match [:a :b])}) => (list (m/create-match :a [0]) (m/create-match :b [1]))
  (walk [:selector [:index "*"]
         [:path [[:child] [:key "foo"]]]]
        {:current
         (m/match [{:foo 1} {:foo 2}])}) => (list (m/create-match 1 [0 :foo]) (m/create-match 2 [1 :foo]))
  (walk [:selector [:filter [:eq
                             [:path [[:current]
                                     [:child]
                                     [:key "bar"]]]
                             [:val "baz"]]]]
        {:current (m/match [{:bar "wrong"} {:bar "baz"}])}) => (list (m/create-match {:bar "baz"} [1]))
  (walk [:path [[:root] [:child] [:key "foo"]]
         [:selector [:filter [:eq [:path [[:current]
                                          [:child]
                                          [:key "bar"]]]
                              [:val "baz"]]]
          [:path [[:child] [:key "hello"]]]]]
        {:root (m/match {:foo [{:bar "wrong" :hello "goodbye"}
                               {:bar "baz" :hello "world"}]})}) => (list (m/create-match "world" [:foo 1 :hello])))

(facts "walking a nil object should be safe"
  (walk [:path [[:root]]] {:root (m/match nil)}) => (m/create-match nil [])
  (walk [:path [[:root] [:child] [:key "foo"]]] {:root (m/match {:bar "baz"})}) => (m/create-match nil [:foo])
  (walk [:path [[:root] [:child] [:key "foo"] [:child] [:key "bar"]]]
        {:root (m/match {:foo {:baz "hello"}})}) => (m/create-match nil [:foo :bar]))
