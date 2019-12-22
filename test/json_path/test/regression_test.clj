(ns json-path.test.regression-test
  (:use [json-path])
  (:require [yaml.core :as yaml]
            [clojure.data.json :as json]
            [clojure
             [test :refer :all]]))

(defn queries_from_suite [suite-yaml]
  (:queries (yaml/from-file suite-yaml)))

(deftest regression
  (let [non-consensus-query-ids (->> (queries_from_suite "test/Clojure_json-path.yaml")
                                     (map :id)
                                     set)]
    (->> (queries_from_suite "test/consensus.yaml")
         (remove (fn [{id :id}] (contains? non-consensus-query-ids id)))
         (map (fn [{:keys [id selector document ordered] :as query}]
                (let [expected (if (contains? query :scalar-consensus)
                                 (:scalar-consensus query)
                                 (:consensus query))
                      current (json-path/at-path selector document)
                      current-reordered (if (= ordered false)
                                          (sort-by json/write-str current)
                                          current)]
                  (testing id
                    (is (= expected
                           current-reordered))))))
         doall)))

(defn- report-change [current-reordered status result {:keys [selector document consensus id]}]
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
  (let [all-queries (queries_from_suite "test/consensus.yaml")
        query-lookup (zipmap (map :id all-queries)
                             all-queries)]
    (->> (queries_from_suite "test/Clojure_json-path.yaml")
       (map (fn [{:keys [id status result] :as query}]
              (let [{:keys [selector document ordered]} (get query-lookup id)]
               (testing id
                (try
                  (let [current (doall (json-path/at-path selector document))
                        current-reordered (if (= ordered false)
                                            (sort-by json/write-str current)
                                            current)]
                    (when (or (= "error" status)
                              (not= result current-reordered))
                      (report-change current-reordered status result (get query-lookup id))))
                  (catch Exception e
                    (when (not= "error"
                                status)
                      (do
                        (println (format "Warning: implementation has changed to error for %s: %s (status %s)" id selector status))
                        (println (format "         was    %s" (pr-str result)))))))))))
       doall)))
