(ns datomic-schematode.constraints-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [datomic-schematode.constraints :as ds-constraints]))

(deftest constraints-tests
  (testing "unique"
    ;; #db/fn breaks the reader, and this (str (sort (into ...))) was
    ;; the only way around it that I could think of. Anybody have a
    ;; better idea?
    (is (= (str (sort (into [] (ds-constraints/unique :snowflake :size :shape :favorite-dolphin))))
           "([:db/fn #db/fn{:code \"(if (clojure.core/empty? (datomic.api/q (quote {:where ([?e :snowflake/size ?snowflake-size] [?e :snowflake/shape ?snowflake-shape] [?e :snowflake/favorite-dolphin ?snowflake-favorite-dolphin] [?dup :snowflake/size ?snowflake-size] [?dup :snowflake/shape ?snowflake-shape] [?dup :snowflake/favorite-dolphin ?snowflake-favorite-dolphin] [(not= ?e ?dup)]), :find [?e]}) db)) nil \\\"Uniqueness failed for [:snowflake/size :snowflake/shape :snowflake/favorite-dolphin]\\\")\", :params [db], :requires [], :imports [], :lang :clojure}] [:db/ident :schematode-constraint-fn-unique-snowflake-size-shape-favorite-dolphin] [:schematode-constraint-fn/active true] [:schematode-constraint-fn/desc \"Auto-generated constraint: unique [:snowflake/size :snowflake/shape :snowflake/favorite-dolphin]\"] [:schematode-constraint-fn/name \"schematode-constraint-fn-unique [:snowflake/size :snowflake/shape :snowflake/favorite-dolphin]\"])"))))

