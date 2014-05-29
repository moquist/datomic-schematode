datomic-schematode ![Build Status](https://codeship.io/projects/b470da60-c813-0131-26e8-0efa69ea263d/status)
===============
##### Note: Still pre-release.

<a title="By Bob Goldstein, UNC Chapel Hill http://bio.unc.edu/people/faculty/goldstein/ (Own work) [CC-BY-SA-3.0 (http://creativecommons.org/licenses/by-sa/3.0)], via Wikimedia Commons" href="http://commons.wikimedia.org/wiki/File%3ACelegansGoldsteinLabUNC.jpg"><img width="64" alt="CelegansGoldsteinLabUNC" src="http://upload.wikimedia.org/wikipedia/commons/6/6a/CelegansGoldsteinLabUNC.jpg"/></a>

    They set a schematode on Norville at lunch, in the new deli. It
    was sighted by laser, and fell through an einrosen-door from
    across the street into the mauve threads of his tweed jacket as he
    read the menu. Ascending his torso, it left a microscopic trail of
    its gastropod-derived polymer network gel among the weave. It
    crouched on his shoulder.

    “Focaccia reubens and hold the horsey,” Norville told Benny, the deli man.

    “Actually,” the schematode piped up, filling the room with its shrilly
    precise and insistent voice, “he wants a sandwich without horseradish sauce
    but with corned beef on an herb-topped flatbread, where corned-beef is
    a salt-cured beef product, and the flatbread is…”

    Because Norville had good taste, he was in a good deli. Because he was in a
    good deli, and because the deli happened to be new, it had auto-scrubbers that
    took the schematode out with one well-placed burst of plasma particulate.

    Benny always bought the best and piled it high. The sandwich was delicious. It had no horseradish.

Datomic Schematode takes your concise expression of schema and constraints and expands upon it, so you can d/transact without explaining every detail yourself.  It’s better than a talking worm.

Uses https://github.com/Yuppiechef/datomic-schema .

## Artifact

All artifacts are published to [clojars](https://clojars.org/datomic-schematode). Latest version is `0.1.0-RC3`:

```
[datomic-schematode "0.1.0-RC3"]
```


## Examples
In the following examples, a few details are ellided. Please see
```dev/datomic_schematode/examples/deli_menu.clj``` for the full example code.

#### First, you need to express your schemas. Here's a small schema for a deli menu:
```clj
(ns datomic-schematode.examples.deli-menu
  (:require [datomic-schematode :as dst]
            [datomic-schematode.constraints :as ds-constraints]))

(def schema1
  [{:namespace :sandwich
    :attrs [[:name :string :indexed]
            [:bread :enum [:focaccia :wheat :maize :rice] :indexed]
            [:meat :string "Many people like meat on their sandwiches"]
            [:needs-toothpick :boolean]]}
   {:namespace :salad
    :attrs [[:name :string :indexed]
            [:base :enum [:lettuce :spinach :pasta :unicorns] :indexed]
            [:dressing :enum [:ranch :honey-mustard :italian :ceasar :minoan]]]}])
```
#### Next, load your schema into datomic:
```clj
datomic-schematode.examples.deli-menu> (dst/load-schema! (d/connect db-url) schema1)
;; => (#<promise$settable_future$reify__4958@6af8f1e9: {:db-before datomic.db.Db@72124995, :db-after datomic.db.Db@c5df3f53, :tx-data [#Datum{:e 13194139534316 :a 50 :v #inst "2014-03-15T04:23:47.235-00:00" :tx 13194139534316 :added true}], :tempids {}}> ...)
```
#### Now transact some facts using your new schema:
```clj
datomic-schematode.examples.deli-menu> (d/transact (d/connect db-url)
                                                   [{:db/id (d/tempid :db.part/user)
                                                     :sandwich/name "Norville's #1"
                                                     :sandwich/bread :sandwich.bread/focaccia
                                                     :sandwich/meat "corned beef"
                                                     :sandwich/needs-toothpick true}
                                                    {:db/id (d/tempid :db.part/user)
                                                     :sandwich/name "Thanksgiving Leftovers"
                                                     :sandwich/bread :sandwich.bread/maize
                                                     :sandwich/meat "turkey"
                                                     :sandwich/needs-toothpick false}
                                                    {:db/id (d/tempid :db.part/user)
                                                     :salad/name "Ceasar"
                                                     :salad/base :salad.base/lettuce
                                                     :salad/dressing :salad.dressing/ceasar}])
;; => #<promise$settable_future$reify__4958@65876428: {:db-before datomic.db.Db@bc569020, :db-after datomic.db.Db@eb44b720, :tx-data ...
```
#### Now you can get your facts back out:
```clj
datomic-schematode.examples.deli-menu> (let [db (d/db (d/connect db-url))
                                             entities (map #(d/touch
                                                             (d/entity db
                                                                       (first %)))
                                                           (d/q '[:find ?e
                                                                  :where [?e :sandwich/bread]] db))]
                                         {:entities entities :count (count entities)})
;; => {:entities ({:sandwich/needs-toothpick true, :sandwich/meat "corned beef", :sandwich/bread :sandwich.bread/focaccia, :sandwich/name "Norville's #1", :db/id 17592186045433}
;; =>             {:sandwich/needs-toothpick false, :sandwich/meat "turkey", :sandwich/bread :sandwich.bread/maize, :sandwich/name "Thanksgiving Leftovers", :db/id 17592186045434}),
;; =>  :count 2}
```

### Using Constraints
#### Constraints Step 1: Express Them
Datomic Schematode enables you to use any db/fn as a constraint, providing that it:

1. returns nil on success (i.e., the constraint is satisifed) and
2. returns a message string on failure.

Suppose that you want to constrain your data such that no sandwich can ever be
named "soap-scum", and such that no two sandwiches can have the same bread and
meat. The "soap-scum" constraint is not likely to be common, so you'll have to
write your own db/fn for that one. But datomic-schematode.constraints/unique can
help you out with multi-attribute uniqueness. Here's how the updated schema
looks:
```clj
(def schema2
  [{:namespace :sandwich
    :attrs [[:name :string :indexed]
            [:bread :enum [:focaccia :wheat :maize :rice] :indexed]
            [:meat :string "Many people like meat on their sandwiches"]
            [:needs-toothpick :boolean]]
    :dbfns [;; We can express any db/fns we want here. If a
            ;; db/fn has the
            ;; :schematode.constraint-fn/active attribute
            ;; with the value true, it will be called as a
            ;; schematode constraint fn, which must return
            ;; either nil (success) or a message explaining
            ;; the violated constraint.
            {:db/ident :my-fn ; The :ident will be namespaced! ...in this case, to :sandwich/my-fn
             :schematode.constraint-fn/active true ; required
             :schematode.constraint-fn/name "Avoid at least one gross sandwich name" ; optional
             :schematode.constraint-fn/desc "Sandwiches with gross names repel customers." ; optional
             :db/fn (d/function '{:lang :clojure
                                  :params [db]
                                  :code (if (empty? (d/q '[:find ?e
                                                           :where [?e :sandwich/name "soap-scum"]]
                                                         db))
                                          nil
                                          "Ew. You are not allowed to name a sandwich \"soap-scum\".")})}
            ;; We can use helper fns to create common constraints.
            (ds-constraints/unique :sandwich :bread :meat)]}
   {:namespace :salad
    :attrs [[:name :string :indexed]
            [:base :enum [:lettuce :spinach :pasta :unicorns] :indexed]
            [:dressing :enum [:ranch :honey-mustard :italian :ceasar :minoan]]]}])
```
#### Constraints Step 2: Transact the necessary Schematode constraints schema and db/fns:
```clj
datomic-schematode.examples.deli-menu> (dst/init-schematode-constraints! (d/connect db-url))
;; => (#<promise$settable_future$reify__4958@7dd81cbd: {:db-before datomic.db.Db@d33b648e, :db-after datomic.db.Db@d4d8c6e7, :tx-data ...)
```
#### Constraints Step 3: Transact your schema with constraints added:
```clj
datomic-schematode.examples.deli-menu> (dst/load-schema! (d/connect db-url) schema2)
;; => (#<promise$settable_future$reify__4958@4ffefcb1: {:db-before datomic.db.Db@36c18235, :db-after datomic.db.Db@7827734f, :tx-data ...)
```
#### Constraints Step 4: Use :schematode/tx for all transactions. (datomic-schematode/tx is a handy wrapper fn you might like for this.)
```clj
datomic-schematode.examples.deli-menu> (d/transact (d/connect db-url)
                                                   [[:schematode/tx :enforce [{:db/id (d/tempid :db.part/user)
                                                                               :sandwich/name "soap-scum"}
                                                                              {:db/id (d/tempid :db.part/user)
                                                                               :sandwich/name "Just Rice"
                                                                               :sandwich/bread :sandwich.bread/rice
                                                                               :sandwich/meat ""}
                                                                              {:db/id (d/tempid :db.part/user)
                                                                               :sandwich/name "Only Rice"
                                                                               :sandwich/bread :sandwich.bread/rice
                                                                               :sandwich/meat ""}]]])
;; => Exception ["Ew. You are not allowed to name a sandwich \"soap-scum\"."]["unique constraint failed for [:sandwich/bread :sandwich/meat]"]  ns-10241/eval10242/fn--10243 (form-init9213208939110354565.clj:1)

;; Or just use datomic-schematode/tx:
datomic-schematode.examples.deli-menu> (dst/tx (d/connect db-url)
                                                   :enforce
                                                   [{:db/id (d/tempid :db.part/user)
                                                     :sandwich/name "soap-scum"}
                                                    {:db/id (d/tempid :db.part/user)
                                                     :sandwich/name "Just Rice"
                                                     :sandwich/bread :sandwich.bread/rice
                                                     :sandwich/meat ""}
                                                    {:db/id (d/tempid :db.part/user)
                                                     :sandwich/name "Only Rice"
                                                     :sandwich/bread :sandwich.bread/rice
                                                     :sandwich/meat ""}])
;; => Exception ["Ew. You are not allowed to name a sandwich \"soap-scum\"."]["unique constraint failed for [:sandwich/bread :sandwich/meat]"]  ns-10241/eval10242/fn--10243 (form-init9213208939110354565.clj:1)
```
#### You can test your constraints without attempting to transact anything. Just pull the :schematode/tx* db/fn out of Datomic and execute it on your transaction data (or use datomic-schematode/tx*, which wraps :schematode/tx* for you):
```clj
datomic-schemtode.examples.deli-menu> (let [my-schematode-tx* (:db/fn (d/entity (d/db (d/connect db-url)) :schematode/tx*))]
                                         (my-schematode-tx* (d/db (d/connect db-url))
                                                            [{:db/id (d/tempid :db.part/user)
                                                              :sandwich/name "soap-scum"}
                                                             {:db/id (d/tempid :db.part/user)
                                                              :sandwich/name "Just Rice"
                                                              :sandwich/bread :sandwich.bread/rice
                                                              :sandwich/meat ""}
                                                             {:db/id (d/tempid :db.part/user)
                                                              :sandwich/name "Only Rice"
                                                              :sandwich/bread :sandwich.bread/rice
                                                              :sandwich/meat ""}]))
;; => ("Ew. You are not allowed to name a sandwich \"soap-scum\"." "unique constraint failed for [:sandwich/bread :sandwich/meat]")

;; Or just use datomic-schematode/tx*:
datomic-schematode.examples.deli-menu> (dst/tx* (d/connect db-url)
                                                    [{:db/id (d/tempid :db.part/user)
                                                      :sandwich/name "soap-scum"}
                                                     {:db/id (d/tempid :db.part/user)
                                                      :sandwich/name "Just Rice"
                                                      :sandwich/bread :sandwich.bread/rice
                                                      :sandwich/meat ""}
                                                     {:db/id (d/tempid :db.part/user)
                                                      :sandwich/name "Only Rice"
                                                      :sandwich/bread :sandwich.bread/rice
                                                      :sandwich/meat ""}])
;; => ("Ew. You are not allowed to name a sandwich \"soap-scum\"." "unique constraint failed for [:sandwich/bread :sandwich/meat]")
```
#### If you want to know about constraint violations, but transact the data anyhow, you can use :warn instead of :enforce when you call :schematode/tx:
```clj
datomic-schematode.examples.deli-menu> (d/transact (d/connect db-url)
                                                   [[:schematode/tx :warn [{:db/id (d/tempid :db.part/user)
                                                                            :sandwich/name "soap-scum"}
                                                                           {:db/id (d/tempid :db.part/user)
                                                                            :sandwich/name "Just Rice"
                                                                            :sandwich/bread :sandwich.bread/rice
                                                                            :sandwich/meat ""}
                                                                           {:db/id (d/tempid :db.part/user)
                                                                            :sandwich/name "Only Rice"
                                                                            :sandwich/bread :sandwich.bread/rice
                                                                            :sandwich/meat ""}]]])
;; => #<promise$settable_future$reify__4958@35e20aca: {:db-before datomic.db.Db@4caaa420, :db-after datomic.db.Db@df9d98cd ...
;; => ... #Datum{:e 13194139534344 :a 74 :v "[\"Ew. You are not allowed to name a sandwich \\\"soap-scum\\\".\"][\"unique constraint failed for [:sandwich/bread :sandwich/meat]\"]" ...
```
Note that the constraint messages have been applied to the TX entity.
#### Analyze costs: you can query the TX entities for the time elapsed while applying schematode constraints, or you can just call datomic-schematode/constraint-cost-stats:
```clj
datomic-schematode.examples.deli-menu> (let [db (d/db (d/connect db-url))
                                             query '[:find ?e :where [?e :schematode.constraint/elapsed-msec]]]
                                         (map #(:schematode.constraint/elapsed-msec (d/entity db (first %)))
                                              (d/q query db)))
;; => (0.001691 0.001691 0.001691 0.001691 0.001691)
datomic-schematode.examples.deli-menu> (dst/constraint-cost-stats (d/connect db-url))
;; => {:mean-msec 0.0016910000000000002, :median-msec 0.001691, :tx-count 5, :standard-deviation-msec 2.42434975903054E-19, :total-msec 0.008455}
```

TODO:
* Add vanilla support for required attrs.

## Thanks
...to [Aaron Brooks](https://github.com/abrooks) for sharing the idea for what became :schematode/tx with me.
