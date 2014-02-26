(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [datomic.api :as d]
            [datomic-schemer.core :as ds-core]
            [datomic-schemer.core-test :as ds-test]))

(def db-url "datomic:mem://testdb")

(defn start!
  "Starts the current development system."
  []
  (d/create-database db-url)
  (map (partial d/transact (d/connect db-url))
       (ds-core/schematize ds-test/test-schemas d/tempid)))

(defn stop!
  "Shuts down and destroys the current development system."
  []
  (d/delete-database db-url))

(defn reset []
  (stop!)
  (refresh :after 'user/start!))
