(ns json-path.test.json-path-test
  [:require [json-path.match :as m]]
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
                          :quuz {:world "zux"}}})  => (list (m/create "bar" [:baz :world]) (m/create "zux" [:baz :quuz :world]))
 (query "$.foo[*]" {:foo ["a", "b", "c"]}) => (list (m/create "a" [:foo 0])
                                                    (m/create "b" [:foo 1])
                                                    (m/create "c" [:foo 2]))
 (query "$.hello" {:hello "world"}) => (m/create "world" [:hello])
 (query "$" {:hello "world"}) => (m/create {:hello "world"} [])
 (query "$.foo[?(@.bar=\"baz\")].hello"
        {:foo [{:bar "wrong" :hello "goodbye"}
               {:bar "baz" :hello "world"}]}) => (list (m/create "world" [:foo 1 :hello])))
