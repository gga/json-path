(ns json-path.test.json-path-test
  [:use
   [json-path]
   [midje.sweet]])

(unfinished)

(def keys-of-many-types
  [{:key 1}
   {:key 3}
   {:key "nice"}
   {:key true}
   {:key nil}
   {:key false}
   {:key {}}
   {:key []}
   {:key -1}
   {:key 0}
   {:key ""}])

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
  (at-path "$[*]" {:foo 1 :bar [2 3]}) => [1 [2 3]]
  (at-path "$..*" {:foo 1 :bar [2 3]}) => [1 [2 3] 2 3]
  (at-path "$[-2]" [1 2 3]) => 2
  (at-path "$[?(@.bar<2)]" [{:bar 1} {:bar 2}]) => [{:bar 1}]
  (at-path "$.foo[?(@.bar=\"baz\")].hello"
           {:foo [{:bar "wrong" :hello "goodbye"}
                  {:bar "baz" :hello "world"}]}) => ["world"]
  (at-path "$.foo[?(@.id=$.id)].text"
           {:id 45, :foo [{:id 12, :text "bar"},
                          {:id 45, :text "hello"}]}) => ["hello"]
  (at-path "$.foo[*].bar[*].baz"
           {:foo [{:bar [{:baz "hello"}]}]}) => ["hello"]
  ;; Filter expression with boolean and operator
  (at-path "$[?(@.key>42 && @.key<44)]"
           [{:key 42}, {:key 43}, {:key 44}]) => [{:key 43}]
  ;; Filter expression with boolean and operator and value false
  (at-path "$[?(@.key>0 && false)]"
           keys-of-many-types) => []
  ;; Filter expression with boolean and operator and value true
  (at-path "$[?(@.key>0 && true)]"
           keys-of-many-types) => [{:key 1}, {:key 3}]
  ;; Filter expression with boolean or operator
  (at-path "$[?(@.key>43 || @.key<43)]"
           [{:key 42}, {:key 43}, {:key 44}]) => [{:key 42}, {:key 44}]
  ;; Filter expression with boolean or operator and value false
  (at-path "$[?(@.key>0 || false)]"
           keys-of-many-types) => [{:key 1}, {:key 3}]
  ;; Filter expression with boolean or operator and value true
  (at-path "$[?(@.key>0 || true)]"
           keys-of-many-types) => keys-of-many-types
  ;; Filter expression with value true
  (at-path "$[?(@.key)]"
           [{:some "some value"}
            {:key true}
            {:key false}
            {:key nil}
            {:key "value"}
            {:key ""}
            {:key 0}
            {:key 1}
            {:key -1}
            {:key 42}
            {:key {}}
            {:key []}])
  => [{:key true}
      {:key false}
      {:key "value"}
      {:key ""}
      {:key 0}
      {:key 1}
      {:key -1}
      {:key 42}
      {:key {}}
      {:key []}])
  ;; TODO: Filter expression with different grouped operators
  ;; NOTE: the parser will need to be updated in order to get this test to work
  ; (at-path "$[?(@.a && (@.b || @.c))]"
  ;          [{:a true}
  ;           {:a true, :b true}
  ;           {:a true, :b true, :c true}
  ;           {:b true, :c true}
  ;           {:a true, :c true}
  ;           {:c true}
  ;           {:b true}])
  ; => [{:a true, :b true}
  ;     {:a true, :b true, :c true}
  ;     {:a true, :c true}])

(facts
  (-> (query "$.hello"
             {:hello "world"})
      :path) => [:hello]
  (-> (query "$"
             {:hello "world"})
      :path) => []
  (->> (query "$..world" {:baz {:world "bar",
                                :quuz {:world "zux"}}})
       (map :value))  => '("bar" "zux")
  (->> (query "$..world" {:baz {:world "bar",
                                :quuz {:world "zux"}}})
       (map :path))  => '([:baz :world] [:baz :quuz :world])
  (->> (query "$.foo[*]" {:foo ["a", "b", "c"]})
       (map :path)) => '([:foo 0] [:foo 1] [:foo 2])
  (-> (query "$.hello"
             {:hello "world"})
      :value) => "world"
  (-> (query "$.hello/world"
             {:hello/world "foo"})
      :value) => "foo"
  (-> (query "$.hello/world.world/name"
              {:hello/world {:world/name "earth"}})
      :value) => "earth"
  (->> (query "$.foo[?(@.bar=\"baz\")].hello"
              {:foo [{:bar "wrong" :hello "goodbye"}
                     {:bar "baz" :hello "world"}]})
       (map :path)) => '([:foo 1 :hello])
  (->> (query "$[?(@.key>42 && @.key<44)]"
              [{:key 42}, {:key 43}, {:key 44}])
       (map :value)) => [{:key 43}]
  (->> (query "$[?(@.key>43 || @.key<43)]"
              [{:key 42}, {:key 43}, {:key 44}])
       (map :value)) => [{:key 42}, {:key 44}])
