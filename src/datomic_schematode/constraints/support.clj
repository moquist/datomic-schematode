(ns datomic-schematode.constraints.support
  (:require [datomic.api :as d]))

(defmacro msec-timer
  "Evaluates expr and returns a vector with the elapsed msec and the value of expr.
   Blatantly rips off clojure.core/time."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr
         em# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
     [em# ret#]))

(def constraints-schema
  [{:namespace :schematode.constraint-fn
    :attrs [[:name :string :indexed]
            [:desc :string]
            [:active :boolean "All db/fns with :schematode-constraint-fn/active = true will be executed by :schematode-tx"]]}

   {:namespace :schematode.constraint
    :attrs [[:constraint/messages :string "messages from :schematode/tx*" :indexed]
            [:constraint/elapsed-msec :double "elapsed time applying schematode constraint fns"]]}

   {:namespace :schematode
    :dbfns
    [{:db/ident :tx*
      :db/fn
      (d/function
       '{:lang :clojure
         :doc
         "([db txs])
          Apply all active schematode constraints and return nil
          (success) or constraint messages (failure).

          Use this fn directly if you wish only to test your txs
          without transacting anything."
         :params [db txs]
         :code (let [wdb (:db-after (d/with db txs))
                     constraints (map first
                                      (d/q '[:find ?e
                                             :where [?e :schematode.constraint-fn/active true]] wdb))
                     msgs (if (empty? constraints)
                            nil
                            (map (fn schematode-tx2 [c] (d/invoke wdb c wdb)) constraints))]
                 (if (every? nil? msgs)
                   nil
                   (remove nil? msgs)))})}
     {:db/ident :tx-timed*
      :db/fn (d/function `{:lang :clojure
                           :doc "([db txs])

                                 Time the application of all active
                                 schematode constraints, return vector
                                 with msgs (which will be nil if
                                 successful) and elapsed time in
                                 msecs."
                           :params ~['db 'txs]
                           :code ~(msec-timer '(d/invoke db :schematode/tx* db txs))})}

     {:db/ident :tx
      :db/fn
      (d/function
       '{:lang :clojure
         :doc
         "([db enforcement txs])
          Apply all active schematode constraints. Transact if
          (success). Else, if (= :warn enforcement), transact and
          assert :schematode.constraint/messages on the tx. Else, if
          (= :enforce enforcement), throw exception with constraint
          messages.

          Always asserts :schematode.constraint/elapsed-msec on the
          tx.

          Note that warning once and then continuing without resolving
          the constraint issues will result in the same constraint
          messages being added to all future txs executed with :warn."
         :params [db enforcement txs]
         :code (let [txid (d/tempid :db.part/tx)
                     [et result] (d/invoke db :schematode/tx-timed* db txs)
                     txs (conj (into [] txs) {:db/id txid :schematode.constraint/elapsed-msec et})]
                 (if (nil? result)
                   txs
                   (let [result (map vector result)
                         result (str (reduce str result))]
                     (if (= :warn enforcement)
                       (conj txs {:db/id txid
                                  ;; Would it be better to assert constraint fn
                                  ;; :ident vals instead of string messages on the
                                  ;; TX entity?
                                  :schematode.constraint/messages result})
                       (throw (Exception. result))))))})}]}])
