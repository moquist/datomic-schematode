(ns datomic-schematode.constraints-test.config
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [datomic-schematode.core :as ds-core]
            [datomic-schematode.constraints :as ds-constraints]))

(def db-url "datomic:mem://constraints-test")

(def test-schemas
  [{:namespace :a
    :attrs [[:a1 :string]
            [:a2 :string]]
    :dbfns [(ds-constraints/unique :a :a1 :a2)]}])

(defn with-schema [f]
  (d/create-database db-url)
  (ds-core/init-schematode-constraints! (d/connect db-url))
  (ds-core/load-schema! (d/connect db-url) test-schemas)
  (f)
  (d/delete-database db-url))

