(ns json-path
  [:require [json-path.parser :as parser]
   [json-path.walker :as walker]])

(defn at-path [path object]
  (walker/walk (parser/parse-path path) object))
