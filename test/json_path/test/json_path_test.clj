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
 (query "$..world" {:baz {:world "bar",
                          :quuz {:world "zux"}}})  => '(["bar" [:baz :world]] ["zux" [:baz :quuz :world]])
 (query "$.foo[*]" {:foo ["a", "b", "c"]}) => '(["a" [:foo 0]] ["b" [:foo 1]] ["c" [:foo 2]])
 (query "$.hello" {:hello "world"}) => ["world" [:hello]]
 (query "$" {:hello "world"}) => [{:hello "world"} []]
 (query "$.foo[?(@.bar=\"baz\")].hello"
        {:foo [{:bar "wrong" :hello "goodbye"}
               {:bar "baz" :hello "world"}]}) => '(["world" [:foo 1 :hello]]))
