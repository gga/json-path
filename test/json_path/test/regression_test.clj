(ns json-path.test.regression-test
  (:use [json-path])
  (:require [yaml.core :as yaml]
            [clojure
             [test :refer :all]]))

(defn queries_from_suite [suite-yaml]
  (:queries (yaml/from-file suite-yaml)))

(deftest regression
  (->> (queries_from_suite "test/Clojure_json-path.yaml")
       (filter (fn [{status :status}] (= status "pass")))
       (map (fn [{:keys [selector document result scalar id]}]
              (testing id
                (is (= (cond-> result
                         scalar first)
                       (json-path/at-path selector document))))))
       doall))
