(ns datomic-schematode.constraints
  (:require [clojure.string :as string]
            [datomic.api :as d]))

(defn- hyphenate-seq
  "Join a seq of name/symbol/keyword values with -."
  [s]
  (string/join "-" (map name s)))

(defn- namespace-attrs
  "Given a namespace string and a seq of attrs, return a seq of namespaced attrs."
  [namespace attrs]
  (map #(keyword namespace (name %)) attrs))

(defn attr->clause
  "Return a d/q where-clause where ebind is the entity-binding symbol
   and attr is bound to a datomic query variable of the same name."
  [ebind attr]
  (let [namespace (namespace attr)
        vbind (str namespace "-" (name attr))
        vbind (symbol (str '? vbind))]
    [ebind attr vbind]))

(defn unique-attrs->clauses
  "Return d/q where-clauses to ensure that there are no distinct
   entities with equal values for all of the specified attrs."
  [namespace attrs]
  (let [attrs (namespace-attrs namespace attrs)
        orig (map #(attr->clause '?e %) attrs)
        dup (map #(attr->clause '?dup %) attrs)]
    (concat
     orig
     dup
     ['[(not= ?e ?dup)]])))

(defn namify
  "Given a base-name (e.g., \"unique\") for this constraint, a single
   datomic namespace and the relevant attributes under that namespace,
   return a vector with the :db/ident, :schematode-constraint/name,
   :schematode-constraint/desc, and failure-message values."
  [base namespace attrs]
  (let [base (name base)
        longbase (str "schematode-constraint-" base)
        ident (keyword (hyphenate-seq (concat [longbase namespace] attrs)))
        ns-attrs (vec (namespace-attrs namespace attrs))
        n (str longbase " " ns-attrs)
        desc (format "Auto-generated constraint: %s %s" base ns-attrs)
        failure-msg (str "Uniqueness failed for " ns-attrs)]
    [ident n desc failure-msg]))

(defn unique
  "Given a single datomic namespace and two or more attrs under it,
   return an entity map for a db/fn to test for uniqueness across
   attrs."
  [namespace & attrs]
  (let [namespace (name namespace)
        [ident n desc failure-msg] (namify :unique namespace attrs)
        where-clauses (unique-attrs->clauses namespace attrs)
        ;; TODO: keep the duplicated attr values from the query result
        ;; and append them to the failure message.
        query {:find ['?e] :where where-clauses}]
    {:db/ident ident
     :schematode-constraint/desc desc
     :schematode-constraint/name n
     :db/fn (d/function `{:lang :clojure
                          :params [~'db]
                          :code (if (empty? (d/q '{:find [~'?e] :where ~where-clauses} ~'db))
                                  nil
                                  ~failure-msg)})}))

(comment
  (d/transact (d/connect db-url) [[:schematode-tx [{:db/id (d/tempid :db.part/user)
                                                    :user/username "fflam"
                                                    :user/lastname "flam"
                                                    :user/dob "2010-01-01"}]]])

  (pprint (map #(d/touch
                 (d/entity
                  (d/db (d/connect db-url))
                  (first %)))
               (d/q '[:find ?e :where [?e :user/username]]
                    (d/db (d/connect db-url)))))

  (d/touch
   (d/entity (d/db (d/connect db-url))
             (ffirst
              (d/q '{:find [?e]
                     :where [[?e :schematode-constraint/name]]}
                   (d/db (d/connect db-url))))))

  )
