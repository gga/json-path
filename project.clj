(defproject json-path "1.0.1"
  :description "JSON Path for Clojure data structures"
  :url "http://github.com/gga/json-path"
  :license "Eclipse Public License 1.0"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                   :plugins [[lein-midje "3.2.1"]]}})
