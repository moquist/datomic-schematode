(ns datomic-schematode.core-test.config
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [datomic-schematode.core :as ds-core]))

(def db-url "datomic:mem://testdb")

(defn with-db [f]
  (d/create-database db-url)
  (f)
  (d/delete-database db-url))
