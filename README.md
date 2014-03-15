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
```test/datomic_schematode/examples/deli_menu.clj``` for the full example code.

First, you need to express your schemas. Here's a simple, single schema for a deli menu:
```clj
(ns datomic-schematode.examples.deli-menu
  (:require [datomic-schematode.core :as ds-core]))

(def schema1
  [:sandwich {:attrs [[:bread-name :string :indexed]
                      [:meat :string "Many people like meat on their sandwiches"]
                      [:needs-toothpick :boolean]]}])
```

Next, load your schema into datomic:
```clj
(ds-core/load-schema! db-conn schema1)
```

Now transact some facts using your new schema:
```clj
(d/transact db-conn
            [{:db/id #db/id[:db.part/user]
              :sandwich/bread-name "focaccia"
              :sandwich/meat "corned beef"
              :sandwich/needs-toothpick true}
             {:db/id #db/id[:db.part/user]
              :sandwich/bread-name "rye"
              :sandwich/meat "turky"
              :sandwich/needs-toothpick false}])
```

Get your facts back out:
```clj
(let [db (d/db db-conn)]
  (pprint (map #(d/touch
                 (d/entity db
                           (first %)))
               (d/q '[:find ?e
                      :where [?e :sandwich/bread-name]] db))))
```

TODO before release:
* Add vanilla support for required attrs.
