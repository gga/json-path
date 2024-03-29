(ns json-path.test.parser-test
  [:use [json-path.parser]
   [midje.sweet]])

(unfinished)

(facts
  (extract-sub-tree 4 8 (range 4 10)) => [5 6 7])

(fact
  (parse-expr  '("@" "." "foo" "=" "\"" "baz" "\""))
  =>
  [:eq [:path [[:current] [:child] [:key "foo"]]] [:val "baz"]])

(facts "comparator expressions should be parseable"
  (parse-expr '("\"" "bar" "\"" "!=" "\"" "bar" "\"")) => [:neq [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "<" "\"" "bar" "\"")) => [:lt [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "<=" "\"" "bar" "\"")) => [:lt-eq [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" ">" "\"" "bar" "\"")) => [:gt [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" ">=" "\"" "bar" "\"")) => [:gt-eq [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "=" "42")) => [:eq [:val "bar"] [:val 42]]
  (parse-expr '("\"" "bar" "\"" "=" "3.1415")) => [:eq [:val "bar"] [:val 3.1415]])

(facts "boolean expressions should be parseable"
  (parse-expr '("\"" "bar" "\"" "&&" "\"" "bar" "\"")) => [:and [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "&&" "true")) => [:and [:val "bar"] [:val true]]
  (parse-expr '("false" "&&" "\"" "bar" "\"")) => [:and [:val false] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "||" "\"" "bar" "\"")) => [:or [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "=" "42" "&&" "\"" "bar" "\"")) => [:and [:eq [:val "bar"] [:val 42]] [:val "bar"]])

(fact
  (parse-indexer '("*")) => [:index "*"]
  (parse-indexer '("3")) => [:index "3"]
  (parse-indexer '("?(" "\"" "bar" "\"" "=" "\"" "bar" "\"" ")")) => [:filter [:eq [:val "bar"] [:val "bar"]]])

(facts
  (parse '("\"" "bar" "\"")) => [:val "bar"]
  (parse '("$")) => [:path [[:root]]]
  (parse '("$" "." "*")) => [:path [[:root] [:child] [:key "*"]]]
  (parse '("$" "." "foo" "[" "3" "]")) => [:path [[:root] [:child] [:key "foo"]] [:selector [:index "3"]]]
  (parse '("$" "[" "3" "]" "." "bar")) => [:path [[:root]] [:selector [:index "3"] [:path [[:child] [:key "bar"]]]]])

(facts
  (parse-path "") => []
  (parse-path "$") => [:path [[:root]]]
  (parse-path "$.hello") => [:path [[:root] [:child] [:key "hello"]]]
  (parse-path "$.hello-world") => [:path [[:root] [:child] [:key "hello-world"]]]
  (parse-path "$.hello/world") => [:path [[:root] [:child] [:key "hello/world"]]]
  (parse-path "$.*") => [:path [[:root] [:child] [:key "*"]]]
  (parse-path "$..hello") => [:path [[:root] [:all-children] [:key "hello"]]]
  (parse-path "$.foo[3]") => [:path [[:root] [:child] [:key "foo"]] [:selector [:index "3"]]]
  (parse-path "foo[*]") => [:path [[:key "foo"]] [:selector [:index "*"]]]
  (parse-path "$[?(@.baz)]") => [:path [[:root]]
                                 [:selector [:filter [:some [:path [[:current]
                                                                    [:child]
                                                                    [:key "baz"]]]]]]]
  (parse-path "$[?(@.bar<2)]") => [:path [[:root]]
                                   [:selector [:filter [:lt
                                                        [:path [[:current]
                                                                [:child]
                                                                [:key "bar"]]]
                                                        [:val 2]]]]]
  (parse-path "$[?(@.bar>42 && @.bar<44)]") => [:path [[:root]]
                                                [:selector [:filter
                                                            [:and
                                                             [:gt
                                                              [:path [[:current]
                                                                      [:child]
                                                                      [:key "bar"]]]
                                                              [:val 42]]
                                                             [:lt
                                                              [:path [[:current]
                                                                      [:child]
                                                                      [:key "bar"]]]
                                                              [:val 44]]]]]]
  (parse-path "$[?(@.bar>42 && true)]") => [:path [[:root]]
                                            [:selector [:filter
                                                        [:and
                                                         [:gt
                                                          [:path [[:current]
                                                                  [:child]
                                                                  [:key "bar"]]]
                                                          [:val 42]]
                                                         [:val true]]]]]
  (parse-path "$[?(@.bar>42 || @.bar<44)]") => [:path [[:root]]
                                                [:selector [:filter
                                                            [:or
                                                             [:gt
                                                              [:path [[:current]
                                                                      [:child]
                                                                      [:key "bar"]]]
                                                              [:val 42]]
                                                             [:lt
                                                              [:path [[:current]
                                                                      [:child]
                                                                      [:key "bar"]]]
                                                              [:val 44]]]]]]
  (parse-path "$.foo[?(@.bar=\"baz\")].hello") => [:path [[:root] [:child] [:key "foo"]]
                                                   [:selector [:filter [:eq [:path [[:current]
                                                                                    [:child]
                                                                                    [:key "bar"]]]
                                                                        [:val "baz"]]]
                                                    [:path [[:child] [:key "hello"]]]]])

(facts "equality tokens should be recognised"
  (parse-path "10!=11") => truthy
  (provided
    (parse (contains "!=")) => true)
  (parse-path "10=11") => truthy
  (provided
    (parse (contains "=")) => true)
  (parse-path "10<11") => truthy
  (provided
    (parse (contains "<")) => true)
  (parse-path "10<=11") => truthy
  (provided
    (parse (contains "<=")) => true)
  (parse-path "10>11") => truthy
  (provided
    (parse (contains ">")) => true)
  (parse-path "10>=11") => truthy
  (provided
    (parse (contains ">=")) => true))
