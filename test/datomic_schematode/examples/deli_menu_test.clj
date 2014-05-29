(ns datomic-schematode.examples.deli-menu-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [datomic-schematode.examples.deli-menu :as deli-menu]
            [datomic-schematode.examples.deli-menu-test.config :as config]
            [datomic-schematode.testslib :as ds-tl]))

(use-fixtures :each config/with-db)

(defmacro should-throw
  "Borrowed from https://github.com/Datomic/day-of-datomic .
   Runs forms, expecting an exception. Returns exception message if an
   exception occurred, and false if no exception occurred."
  [& forms]
  `(try
     ~@forms
     false
     (catch Exception t#
       (str "Got expected exception:\n\t" (.getMessage t#)))))

(deftest deli-menu-tests-no-constraints
  (testing "step2"
    (is (ds-tl/ensure-seq-txs (deli-menu/step2))))
  (testing "step3"
    (is (ds-tl/ensure-tx (deli-menu/step3))))
  (testing "step4"
    (is (= (let [{:keys [entities count]} (deli-menu/step4)
                 entities (map ds-tl/predict-entity-map entities)
                 entities (into #{} entities)]
             [entities count])
           [#{{:sandwich/needs-toothpick true, :sandwich/meat "corned beef", :sandwich/bread :sandwich.bread/focaccia, :sandwich/name "Norville's #1", :db/id -1} {:sandwich/needs-toothpick false, :sandwich/meat "turkey", :sandwich/bread :sandwich.bread/maize, :sandwich/name "Thanksgiving Leftovers", :db/id -1}} 2]))))

(deftest deli-menu-tests-constraints
  (testing "step6"
    (is (ds-tl/ensure-seq-txs (deli-menu/step6))))
  (testing "step7"
    (is (ds-tl/ensure-seq-txs (deli-menu/step7))))
  (testing "step8"
    (is (= (ds-tl/should-throw @(deli-menu/step8))
           "Got expected exception:\n\tjava.lang.Exception: [\"Ew. You are not allowed to name a sandwich \\\"soap-scum\\\".\"][\"unique constraint failed for [:sandwich/bread :sandwich/meat]\"]")))
  (testing "step9"
    (is (= (sort (deli-menu/step9))
           '("Ew. You are not allowed to name a sandwich \"soap-scum\"."
             "unique constraint failed for [:sandwich/bread :sandwich/meat]"))))
  
  (testing "step10 and step11"
    (is (= (sort (keys (first (do
                                (deli-menu/step10)
                                (deli-menu/step11)))))
           '(:db/txInstant :schematode.constraint/elapsed-msec :schematode.constraint/messages))))

  (testing "step12"
    (is (every? number? (deli-menu/step12))))

  (testing "step13"
    (is (= (sort (keys (deli-menu/step13)))
           '(:mean-msec :median-msec :total-msec :tx-count)))))
