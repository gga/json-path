(ns json-path.test.json-path-test
  [:use [json-path]
   [midje.sweet]])

(unfinished)

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
  (at-path "$.foo[?(@.id=$.id)].text"
           {:id 45, :foo [{:id 12, :text "bar"},
                          {:id 45, :text "hello"}]}) => ["hello"])

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
  (->> (query "$.foo[?(@.bar=\"baz\")].hello"
              {:foo [{:bar "wrong" :hello "goodbye"}
                     {:bar "baz" :hello "world"}]})
       (map :path)) => '([:foo 1 :hello]))
