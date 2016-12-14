(ns json-path
  [:require [json-path.parser :as parser]
   [json-path.walker :as walker]])

(defn query [path object]
  (walker/walk (parser/parse-path path) {:root object}))

(defn at-path [path object]
  (walker/map# first (query path object)))
