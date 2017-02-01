(ns json-path.match)

(defrecord Match [path value])

(defn value [match]
  (:value match))

(defn- path [match]
  (:path match))

(defn match
  ([value] (->Match [] value))
  ([key value] (->Match [key] value))
  ([key value context] (->Match (vec (concat (path context) [key]))
                                value)))

(defn create-match [value path]
  (->Match path value))
