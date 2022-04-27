(defproject json-path "2.1.0"
  :description "JSON Path for Clojure data structures"
  :url "http://github.com/gga/json-path"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :profiles {:dev {:dependencies [[midje "1.9.6"]
                                  [org.clojure/data.json "0.2.7"]
                                  [io.forward/yaml "1.0.11"]]
                   :plugins [[lein-midje "3.2.1"]]}})
