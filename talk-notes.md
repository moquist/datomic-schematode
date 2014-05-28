## Southern NH Clojure Group: 2014-05-28

1. Briefly intro datomic: it's not a key-value data store, it's an entity-attribute-value-time datom store.
1. Show datomic schema: https://github.com/Datomic/day-of-datomic/blob/34a4f59810179ae66374682e182279f8e71126b5/resources/day-of-datomic/schema.edn
    1. It's good, because it's Just Data. But I don't want to type all that in every case when I need to express Datomic schema.
1. Schematode does two things for you:
    1. lets you express datomic schema as a concise data literal allowing further programmatic manipulation before transacting, and
        1. I use https://github.com/Yuppiechef/datomic-schema as a library. When I first found it, that lib collected schema in atoms as you built it, and I didn't want that. The author has since deprecated that global state. But it also uses macros to declare your schema, and I just want plain data without any macros involved. Schematode lets you have your plain data, and it eats it, too.
    1. gives you some basic tools for DB-wide constraints
1. Deli menu schema (see https://github.com/vlacs/datomic-schematode)
    1. Schematode is doing very little here -- mostly just calling 'datomic-schema.schema/field-to-datomic
1. Deli menu constraints (see https://github.com/vlacs/datomic-schematode)
1. Constraint cost analysis: (see https://github.com/vlacs/datomic-schematode)
