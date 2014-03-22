datomic-schematode [![Build Status](https://travis-ci.org/vlacs/datomic-schematode.png?branch=master)](https://travis-ci.org/vlacs/datomic-schematode)
===============
*Note: Still pre-release.*

<a title="By Bob Goldstein, UNC Chapel Hill http://bio.unc.edu/people/faculty/goldstein/ (Own work) [CC-BY-SA-3.0 (http://creativecommons.org/licenses/by-sa/3.0)], via Wikimedia Commons" href="http://commons.wikimedia.org/wiki/File%3ACelegansGoldsteinLabUNC.jpg"><img width="64" alt="CelegansGoldsteinLabUNC" src="http://upload.wikimedia.org/wikipedia/commons/6/6a/CelegansGoldsteinLabUNC.jpg"/></a>

    They set a schematode on Norville at lunch, in the new deli. It was sighted
    by laser, and fell through an einrosen-door from across the street into the
    mauve threads of his tweed jacket as he read the menu. It ascended his torso,
    leaving a microscopic trail of its gastropod-derived polymer network gel among
    the weave. It crouched on his shoulder.

    “Focaccia reubens,” Norville told Benny, the deli man.

    “Actually,” the schematode piped up, filling the room with its shrilly
    precise and insistent voice, “he wants corned beef on an herb-topped flatbread,
    where corned-beef is a salt-cured beef product, and the flatbread is…”

    Because Norville had good taste, he was in a good deli. Because he was in a
    good deli, and because the deli happened to be new, it had auto-scrubbers that
    took the schematode out with one well-placed burst of plasma particulate.

    Benny always bought the best and piled it high. The sandwich was delicious.

Datomic Schematode takes your concise schema expression and expands upon it, so you can d/transact without explaining every detail yourself. It’s better than a talking worm.

Uses https://github.com/Yuppiechef/datomic-schema .

## Usage
*These instructions are incomplete and preliminary.*

In the following example, a few details are ellided. Please see
```dev/datomic_schematode/examples/deli_menu.clj``` for the full example code.

First, you need to express your schemas. Here's a simple, single schema for a deli menu:
```clj
(ns datomic-schematode.examples.deli-menu
  (:require [datomic-schematode.core :as ds-core]))

(def schema1
  [[:sandwich {:attrs [[:name :string :indexed]
                       [:bread :enum [:focaccia :wheat :maize :rice] :indexed]
                       [:meat :string "Many people like meat on their sandwiches"]
                       [:needs-toothpick :boolean]]}]
   [:salad {:attrs [[:name :string :indexed]
                    [:base :enum [:lettuce :spinach :pasta :unicorns] :indexed]
                    [:dressing :enum [:ranch :honey-mustard :italian :ceasar :minoan]]]}]])
```

Next, load your schema into datomic:
```clj
datomic-schematode.examples.deli-menu> (ds-core/load-schema! db-conn schema1)
;; => (#<promise$settable_future$reify__4958@6af8f1e9: {:db-before datomic.db.Db@72124995, :db-after datomic.db.Db@c5df3f53, :tx-data [#Datum{:e 13194139534316 :a 50 :v #inst "2014-03-15T04:23:47.235-00:00" :tx 13194139534316 :added true}], :tempids {}}> ...)
```

Now transact some facts using your new schema:
```clj
datomic-schematode.examples.deli-menu> (d/transact db-conn
                                                   [{:db/id #db/id[:db.part/user]
                                                     :sandwich/name "Norville's #1"
                                                     :sandwich/bread :sandwich.bread/focaccia
                                                     :sandwich/meat "corned beef"
                                                     :sandwich/needs-toothpick true}
                                                    {:db/id #db/id[:db.part/user]
                                                     :sandwich/name "Thanksgiving Leftovers"
                                                     :sandwich/bread :sandwich.bread/maize
                                                     :sandwich/meat "turkey"
                                                     :sandwich/needs-toothpick false}
                                                    {:db/id #db/id[:db.part/user]
                                                     :salad/name "Ceasar"
                                                     :salad/base :salad.base/lettuce
                                                     :salad/dressing :salad.dressing/ceasar}])
;; => #<promise$settable_future$reify__4958@65876428: {:db-before datomic.db.Db@bc569020, :db-after datomic.db.Db@eb44b720, :tx-data ...
```

Now you can get your facts back out:
```clj
datomic-schematode.examples.deli-menu> (let [db (d/db db-conn)
                                             entities (map #(d/touch
                                                             (d/entity db
                                                                       (first %)))
                                                           (d/q '[:find ?e
                                                                  :where [?e :sandwich/bread]] db))]
                                         (pprint entities)
                                         (count entities))
;; => ({:sandwich/needs-toothpick true, :sandwich/meat "corned beef", :sandwich/bread :sandwich.bread/focaccia, :sandwich/name "Norville's #1", :db/id 17592186045433}
;; =>  {:sandwich/needs-toothpick false, :sandwich/meat "turkey", :sandwich/bread :sandwich.bread/maize, :sandwich/name "Thanksgiving Leftovers", :db/id 17592186045434})
;; => 2
```

TODO before release:
* Add vanilla support for required attrs.
* Document schematode-tx and general constraints support.
