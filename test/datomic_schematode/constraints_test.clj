(ns datomic-schematode.constraints-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [datomic-schematode.core :as ds-core]
            [datomic-schematode.constraints :as ds-constraints]
            [datomic-schematode.constraints-test.config :as config]
            [datomic-schematode.testslib :as ds-tl]))

(use-fixtures :once config/with-schema)

(deftest constraints-tests
  (testing "uniqueness helper fn"
    ;; #db/fn breaks the reader, and this (str (sort (into ...))) was
    ;; the only way around it that I could think of. Anybody have a
    ;; better idea?
    (is (= (str (sort (into [] (ds-constraints/unique :snowflake :size :shape :favorite-dolphin))))
           "([:db/fn #db/fn{:code \"(if (clojure.core/empty? (datomic.api/q (quote {:where ([?e :snowflake/size ?snowflake-size] [?e :snowflake/shape ?snowflake-shape] [?e :snowflake/favorite-dolphin ?snowflake-favorite-dolphin] [?dup :snowflake/size ?snowflake-size] [?dup :snowflake/shape ?snowflake-shape] [?dup :snowflake/favorite-dolphin ?snowflake-favorite-dolphin] [(not= ?e ?dup)]), :find [?e]}) db)) nil \\\"unique constraint failed for [:snowflake/size :snowflake/shape :snowflake/favorite-dolphin]\\\")\", :params [db], :requires [], :imports [], :lang :clojure}] [:db/ident :schematode-constraint-fn-unique-snowflake-size-shape-favorite-dolphin] [:schematode.constraint-fn/active true] [:schematode.constraint-fn/desc \"Auto-generated constraint: unique [:snowflake/size :snowflake/shape :snowflake/favorite-dolphin]\"] [:schematode.constraint-fn/name \"schematode-constraint-fn-unique [:snowflake/size :snowflake/shape :snowflake/favorite-dolphin]\"])")))

  (testing ":schematode/tx"
    (testing ":enforce"
      (is (= (ds-tl/should-throw @(ds-core/tx (d/connect config/db-url) :enforce
                                              [{:db/id (d/tempid :db.part/user) :a/a1 "" :a/a2 ""}
                                               {:db/id (d/tempid :db.part/user) :a/a1 "" :a/a2 ""}]))
             "Got expected exception:\n\tjava.lang.Exception: [\"unique constraint failed for [:a/a1 :a/a2]\"]")))
    (testing ":warn"
      (is (= (keys @(ds-core/tx (d/connect config/db-url) :warn
                                [{:db/id (d/tempid :db.part/user) :a/a1 "" :a/a2 ""}
                                 {:db/id (d/tempid :db.part/user) :a/a1 "" :a/a2 ""}]))
             '(:db-before :db-after :tx-data :tempids)))))

  (testing ":schematode/tx*"
    (is (= (ds-core/tx* (d/connect config/db-url) 
                        [{:db/id (d/tempid :db.part/user) :a/a1 "" :a/a2 ""}
                         {:db/id (d/tempid :db.part/user) :a/a1 "" :a/a2 ""}])
           '("unique constraint failed for [:a/a1 :a/a2]")))))
