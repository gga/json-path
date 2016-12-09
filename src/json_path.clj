(ns json-path
  [:require [json-path.parser :as parser]
   [json-path.walker :as walker]])

(defn at-path [path object]
  (let [result (walker/walk (parser/parse-path path) {:root object})]
    (walker/map# first result)))
