(ns json-path.match)

(defrecord Match [path value])

(defn root [value]
  (->Match [] value))

(defn with-context
  ([key value context] (->Match (vec (concat (:path context) [key]))
                                value)))

(defn create [value path]
  (->Match path value))
