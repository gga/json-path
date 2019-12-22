(ns json-path.test.regression-test
  (:use [json-path])
  (:require [yaml.core :as yaml]
            [clojure.data.json :as json]
            [clojure
             [test :refer :all]]))

(defn queries_from_suite [suite-yaml]
  (:queries (yaml/from-file suite-yaml)))

(defn- query-implementation-for [{:keys [selector document ordered]}]
  (let [current (json-path/at-path selector document)]
    (if (= ordered false)
      (sort-by json/write-str current)
      current)))

(defn- expected-by-consensus [query]
  (if (contains? query :scalar-consensus)
    (:scalar-consensus query)
    (:consensus query)))

(deftest regression
  (let [non-consensus-query-ids (->> (queries_from_suite "test/Clojure_json-path.yaml")
                                     (map :id)
                                     set)]
    (->> (queries_from_suite "test/consensus.yaml")
         (remove (fn [{id :id}] (contains? non-consensus-query-ids id)))
         (map (fn [{:keys [id] :as query}]
                (testing id
                  (is (= (expected-by-consensus query)
                         (query-implementation-for query))))))
         doall)))

(defn- report-result [current-result current-exception previous {:keys [id selector] :as query}]
  (println
   (format "Warning: implementation has changed for %s: %s (previous status %s)"
           id selector (:status previous)))
  (println "Old:")
  (if (= (:status previous) "error")
    (println "  Error")
    (println (format"  %s" (pr-str (:result previous)))))
  (println "New:")
  (if (some? current-exception)
    (println "  Error")
    (println (format"  %s" (pr-str current-result))))
  (when-let [consensus (expected-by-consensus query)]
    (println "Consensus:")
    (println (format"  %s" (pr-str consensus)))))

;; This section is for warning us if we are moving away from previously
;; recorded results which however are not backed by a consensus based
;; on https://github.com/cburgmer/json-path-comparison
(deftest warning-on-changes-for-non-conforming-queries-based-on-consensus
  (let [all-queries (queries_from_suite "test/consensus.yaml")
        query-lookup (zipmap (map :id all-queries)
                             all-queries)]
    (->> (queries_from_suite "test/Clojure_json-path.yaml")
         (map (fn [{:keys [id status result] :as previous}]
                (let [query (get query-lookup id)]
                  (testing id
                    (try
                      (let [current-result (query-implementation-for query)]
                        (when (or (= "error" status)
                                  (not= result current-result))
                          (report-result current-result nil previous query)))
                      (catch Exception e
                        (when (not= "error"
                                    status)
                          (report-result nil e previous query))))))))
       doall)))
