(ns json-path
  [:require [json-path.parser :as parser]
   [json-path.match :as m]
   [json-path.walker :as walker]])

(defn query [path object]
  (walker/walk (parser/parse-path path) {:root (m/root object)}))

(defn at-path [path object]
  (walker/map# :value (query path object)))
