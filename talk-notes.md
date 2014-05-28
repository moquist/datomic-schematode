## Southern NH Clojure Group: 2014-05-28

1. Briefly intro datomic.
1. Show datomic schema: https://github.com/Datomic/day-of-datomic/blob/34a4f59810179ae66374682e182279f8e71126b5/resources/day-of-datomic/schema.edn
    1. It's good, because it's Just Data. But I don't want to type all that in every case when I need to express Datomic schema.
1. Schematode does two things for you:
    1. lets you express datomic schema as a concise data literal, for programmatic manipulation before transacting, and
        1. I use https://github.com/Yuppiechef/datomic-schema as a library. When I first found it, that lib collected schema in atoms as you built it, and I didn't want that. The author has since deprecated that global state. But it also uses macros to declare your schema, and I just want plain data without any macros involved. Schematode lets you have your plain data, and it eats it, too.
    1. gives you some basic tools for DB-wide constraints
1. Deli menu schema
1. Deli menu constraints
