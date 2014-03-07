(ns datomic-schematode.constraints
  (:require [clojure.string :as string]))

(defn seq->str [s]
  (string/join "-" (map name s)))

(defn attr->clause
  "Return a datomic query where clause where ebind is the entity-binding symbol and attr is bound."
  [ebind attr]
  (let [namespace (namespace attr)
        vbind (str namespace "-" (name attr))
        vbind (symbol (str '? vbind))]
    [ebind attr (symbol vbind)]))

(defn attrs->clauses
  "Return datomic query where clauses to ensure that there are no
   distinct entities with equal values for all of the specified attrs."
  [namespace attrs]
  (let [attrs (map #(keyword namespace (name %)) attrs)
        orig (map #(attr->clause '?e %) attrs)
        dup (map #(attr->clause '?dup %) attrs)]
    (concat
     orig
     dup
     ['[(not= ?e ?dup)]])))

(defn unique [namespace & attrs]
  (let [namespace (name namespace)
        ident (keyword namespace (seq->str attrs))
        where-clauses (attrs->clauses namespace attrs)
        failure-msg "hi"]
    {:db/ident ident
     :db/fn `{:params [db]
              :code (if (d/q '[:find ?e :where ~where-clauses] db)
                      failure-msg
                      nil)}}))
