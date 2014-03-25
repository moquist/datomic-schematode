(ns datomic-schematode.examples.deli-menu-test.config
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [datomic-schematode.examples.deli-menu :as deli-menu]))

(defn with-db [f]
  (d/create-database deli-menu/db-url)
  (f)
  (d/delete-database deli-menu/db-url))
