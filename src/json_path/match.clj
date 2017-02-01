(ns json-path.match)

(defn value [match]
  (first match))

(defn- path [match]
  (last match))

(defn match
  ([value] [value []])
  ([key value] [value [key]])
  ([key value context] [value (vec (concat (path context) [key]))]))

(defn create-match [value path]
  [value path])
