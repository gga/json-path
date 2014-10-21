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

(facts "equality expressions should be parseable"
  (parse-expr '("\"" "bar" "\"" "!=" "\"" "bar" "\"")) => [:neq [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "<" "\"" "bar" "\"")) => [:lt [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" "<=" "\"" "bar" "\"")) => [:lt-eq [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" ">" "\"" "bar" "\"")) => [:gt [:val "bar"] [:val "bar"]]
  (parse-expr '("\"" "bar" "\"" ">=" "\"" "bar" "\"")) => [:gt-eq [:val "bar"] [:val "bar"]])

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
  (parse-path "$.*") => [:path [[:root] [:child] [:key "*"]]]
  (parse-path "$..hello") => [:path [[:root] [:all-children] [:key "hello"]]]
  (parse-path "$.foo[3]") => [:path [[:root] [:child] [:key "foo"]] [:selector [:index "3"]]]
  (parse-path "foo[*]") => [:path [[:key "foo"]] [:selector [:index "*"]]]
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
