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
  (testing "step1!"
    (is (ds-tl/ensure-seq-txs (deli-menu/step1!))))
  (testing "step2! and step3"
    (is (= (let [{:keys [entities count]} (do
                                            (deli-menu/step1!)
                                            (deli-menu/step2!)
                                            (deli-menu/step3))
                 entities (map ds-tl/predict-entity-map entities)
                 entities (into #{} entities)]
             [entities count])
           [#{{:sandwich/needs-toothpick true, :sandwich/meat "corned beef", :sandwich/bread :sandwich.bread/focaccia, :sandwich/name "Norville's #1", :db/id -1} {:sandwich/needs-toothpick false, :sandwich/meat "turkey", :sandwich/bread :sandwich.bread/maize, :sandwich/name "Thanksgiving Leftovers", :db/id -1}} 2]))))

(deftest deli-menu-tests-constraints
  (testing "step4!"
    (is (ds-tl/ensure-seq-txs (deli-menu/step4!))))
  (testing "step5!"
    (is (ds-tl/ensure-seq-txs (deli-menu/step5!))))
  (testing "step6!"
    (is (= (ds-tl/should-throw @(deli-menu/step6!))
           "Got expected exception:\n\tjava.lang.Exception: [\"Ew. You are not allowed to name a sandwich \\\"soap-scum\\\".\"][\"unique constraint failed for [:sandwich/bread :sandwich/meat]\"]")))
  (testing "step7"
    (is (= (sort (deli-menu/step7))
           '("Ew. You are not allowed to name a sandwich \"soap-scum\"."
             "unique constraint failed for [:sandwich/bread :sandwich/meat]"))))
  
  (testing "step8! and step9"
    (is (= (keys (first (do
                          (deli-menu/step8!)
                          (deli-menu/step9))))
           '(:schematode.constraint/messages :schematode.constraint/elapsed-msec :db/txInstant))))

  (testing "step10"
    (is (every? number? (deli-menu/step10))))

  (testing "step11"
    (is (= (keys (deli-menu/step11))
           '(:mean-msec :median-msec :tx-count :total-msec)))))
