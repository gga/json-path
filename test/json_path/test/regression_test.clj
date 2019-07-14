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
       (map (fn [{:keys [selector document result id]}]
              (testing id
                (is (= result
                       (json-path/at-path selector document))))))
       doall))

;; This section is for warning us if we are moving away from previously
;; recorded results which however are not backed by a consensus based
;; on https://github.com/cburgmer/json-path-comparison
(deftest warning-on-changes-for-non-conforming-queries-based-on-consensus
  (->> (queries_from_suite "test/Clojure_json-path.yaml")
       (filter (fn [{status :status}] (not= status "pass")))
       (map (fn [{:keys [selector document result status id]}]
              (testing id
                (let [last-recorded result
                      current (json-path/at-path selector document)]
                  (when (not= last-recorded
                              current)
                    (println (format "Warning: result has changed for %s: %s (status %s)" id selector status))
                    (println (format "         was    %s" (pr-str last-recorded)))
                    (println (format "         now is %s" (pr-str current))))))))
       doall))
