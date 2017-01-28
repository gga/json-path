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
  (eval-expr [:path [[:key "foo"]]] {:current (m/root {:foo "bar"})}) => "bar"
  (eval-expr [:eq [:path [[:key "foo"]]] [:val "bar"]] {:current (m/root {:foo "bar"})}) => truthy)

(facts
  (select-by [:key "hello"] (m/root {:hello "world"})) => (m/create "world" [:hello])
  (select-by [:key "hello"] {:current [{:hello "foo"} {:hello "bar"}]}) => (m/create nil [:hello])
  (select-by [:key "*"] (m/root {:hello "world"})) => (list (m/create "world" [:hello]))
  (select-by [:key "*"] (m/root {:hello "world" :foo "bar"})) => (list (m/create "bar" [:foo]) (m/create "world" [:hello]))
  (select-by [:key "*"] (m/root [{:hello "world"} {:foo "bar"}])) => (list (m/create {:hello "world"} [0]) (m/create {:foo "bar"} [1]))
  (select-by [:key "*"] {:current 1}) => '())

(facts
  (walk-path [[:root]] {:root (m/root ...root...), :current  (m/root ...obj...)}) => (m/create ...root... [])
  (walk-path [[:root] [:child] [:key "foo"]] {:root (m/root {:foo "bar"})}) => (m/create "bar" [:foo])
  (walk-path [[:all-children]] {:current (m/root {:foo "bar" :baz {:qux "zoo"}})}) => (list (m/create {:foo "bar" :baz {:qux "zoo"}} [])
                                                                                             (m/create {:qux "zoo"} [:baz]))
  (walk-path [[:all-children] [:key "bar"]]
             {:current (m/root '([{:bar "hello"}]))}) => (list (m/create "hello" [0 0 :bar]))
  (walk-path [[:all-children] [:key "bar"]]
             {:current (m/root {:foo [{:bar "wrong"}
                                       {:bar "baz"}]})}) => (list (m/create "wrong" [:foo 0 :bar])
                                                                  (m/create "baz" [:foo 1 :bar]))
  (walk-path [[:all-children] [:key "foo"]]
             {:current (m/root {:foo [{:foo "foo"}]})}) => (list (m/create [{:foo "foo"}] [:foo])
                                                                  (m/create "foo" [:foo 0 :foo])))

(facts
  (walk-selector [:index "1"] {:current (m/root ["foo", "bar", "baz"])}) => (m/create "bar" [1])
  (walk-selector [:index "*"] {:current (m/root [:a :b])}) => (list (m/create :a [0]) (m/create :b [1]))
  (walk-selector [:filter [:eq [:path [[:current] [:child] [:key "bar"]]] [:val "baz"]]]
                 {:current (m/root [{:bar "wrong"} {:bar "baz"}])}) => (list (m/create {:bar "baz"} [1])))

(fact "selecting places constraints on the shape of the object being selected from"
  (walk-selector [:index "1"] {:current (m/root {:foo "bar"})}) => (throws Exception)
  (walk-selector [:index "*"] {:current (m/root {:foo "bar"})}) => (throws Exception))

(facts
  (walk [:path [[:root]]] {:root (m/root ...json...)}) => (m/create ...json... [])
  (walk [:path [[:child]]] {:current (m/root ...json...)}) => (m/create ...json... [])
  (walk [:path [[:current]]] {:current (m/root ...json...)}) => (m/create ...json... [])
  (walk [:path [[:key "foo"]]] {:current (m/root {:foo "bar"})}) => (m/create "bar" [:foo])
  (walk [:path [[:all-children]]]
        {:current
         (m/root {:hello {:world "foo"},
                   :baz {:world "bar",
                         :quuz {:world "zux"}}})}) => (list (m/create {:hello {:world "foo"},
                                                                       :baz {:world "bar", :quuz {:world "zux"}}}
                                                                      [])
                                                            (m/create {:world "foo"}
                                                                      [:hello])
                                                            (m/create {:world "bar",
                                                                       :quuz {:world "zux"}}
                                                                      [:baz])
                                                            (m/create {:world "zux"}
                                                                      [:baz :quuz]))
  (walk [:path [[:all-children]]]
        {:current
         (m/root (list {:hello {:world "foo"}}
                        {:baz {:world "bar"}}))}) => (list (m/create [{:hello {:world "foo"}}
                                                                      {:baz {:world "bar"}}]
                                                                     [])
                                                           (m/create {:hello {:world "foo"}} [0])
                                                           (m/create {:world "foo"} [0 :hello])
                                                           (m/create {:baz {:world "bar"}} [1])
                                                           (m/create {:world "bar"} [1 :baz]))
  (walk [:path [[:all-children]]]
        {:current (m/root "scalar")}) => (list (m/create "scalar" []))
  (walk [:path [[:all-children] [:key "world"]]]
        {:current (m/root {:hello {:world "foo"},
                            :baz   {:world "bar",
                                    :quuz {:world "zux"}}})}) => (list (m/create "foo" [:hello :world])
                                                                       (m/create "bar" [:baz :world])
                                                                       (m/create "zux" [:baz :quuz :world]))
  (walk [:selector [:index "1"]] {:current (m/root ["foo", "bar", "baz"])}) => (m/create "bar" [1])
  (walk [:selector [:index "*"]] {:current (m/root [:a :b])}) => (list (m/create :a [0]) (m/create :b [1]))
  (walk [:selector [:index "*"]
         [:path [[:child] [:key "foo"]]]]
        {:current
         (m/root [{:foo 1} {:foo 2}])}) => (list (m/create 1 [0 :foo]) (m/create 2 [1 :foo]))
  (walk [:path [[:key "*"]]] {:current 1}) => '()
  (walk [:selector [:filter [:eq
                             [:path [[:current]
                                     [:child]
                                     [:key "bar"]]]
                             [:val "baz"]]]]
        {:current (m/root [{:bar "wrong"} {:bar "baz"}])}) => (list (m/create {:bar "baz"} [1]))
  (walk [:path [[:root] [:child] [:key "foo"]]
         [:selector [:filter [:eq [:path [[:current]
                                          [:child]
                                          [:key "bar"]]]
                              [:val "baz"]]]
          [:path [[:child] [:key "hello"]]]]]
        {:root (m/root {:foo [{:bar "wrong" :hello "goodbye"}
                               {:bar "baz" :hello "world"}]})}) => (list (m/create "world" [:foo 1 :hello])))

(facts "walking a nil object should be safe"
  (walk [:path [[:root]]] {:root (m/root nil)}) => (m/create nil [])
  (walk [:path [[:root] [:child] [:key "foo"]]] {:root (m/root {:bar "baz"})}) => (m/create nil [:foo])
  (walk [:path [[:root] [:child] [:key "foo"] [:child] [:key "bar"]]]
        {:root (m/root {:foo {:baz "hello"}})}) => (m/create nil [:foo :bar]))
