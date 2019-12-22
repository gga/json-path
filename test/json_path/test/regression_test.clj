(ns json-path.test.regression-test
  (:use [json-path])
  (:require [yaml.core :as yaml]
            [clojure.data.json :as json]
            [clojure
             [test :refer :all]]))

(defn queries_from_suite [suite-yaml]
  (:queries (yaml/from-file suite-yaml)))

(deftest regression
  (->> (queries_from_suite "test/Clojure_json-path.yaml")
       (filter (fn [{status :status}] (= status "pass")))
       (map (fn [{:keys [selector document result id ordered]}]
              (let [current (json-path/at-path selector document)
                    current-reordered (if (= ordered false)
                                        (sort-by json/write-str current)
                                        current)]
                (testing id
                  (is (= result
                         current-reordered))))))
       doall))

(defn- report-change [current-reordered {:keys [selector document result status consensus id]}]
  (println
   (format "Warning: implementation has changed for %s: %s (previous status %s)"
           id selector status))
  (when (or (nil? consensus)
            (not= consensus current-reordered))
    (when-not (= status "error")
      (println (format "         was           %s" (pr-str result))))
    (println (format "         now is        %s" (pr-str current-reordered))))
  (when-not (nil? consensus)
    (if (= consensus
           current-reordered)
      (println "         now matching consensus")
      (println (format "         but should be %s" (pr-str consensus))))))

;; This section is for warning us if we are moving away from previously
;; recorded results which however are not backed by a consensus based
;; on https://github.com/cburgmer/json-path-comparison
(deftest warning-on-changes-for-non-conforming-queries-based-on-consensus
  (->> (queries_from_suite "test/Clojure_json-path.yaml")
       (filter (fn [{status :status}] (not= status "pass")))
       (map (fn [{:keys [selector document result status consensus id ordered] :as query}]
              (testing id
                (try
                  (let [current (doall (json-path/at-path selector document))
                        current-reordered (if (= ordered false)
                                            (sort-by json/write-str current)
                                            current)]
                    (when (or (= "error" status)
                              (not= result current-reordered))
                      (report-change current query)))
                  (catch Exception e
                    (when (not= "error"
                                status)
                      (do
                        (println (format "Warning: implementation has changed to error for %s: %s (status %s)" id selector status))
                        (println (format "         was    %s" (pr-str result))))))))))
       doall))
