# Introduction to datomic-schematode

TODO: write [great documentation](http://jacobian.org/writing/great-documentation/what-to-write/)

# Schema Creation Reference

This is just a simple reference to help you know what schematode options result in any given datomic schema options

I'm just using what I'm groking from the ["field-to-datomic" function found here](https://github.com/Yuppiechef/datomic-schema/blob/master/src/datomic_schema/schema.clj).

the first two are required so that your schema attribute has both an identity and a value type, giving you two of the required items for defining such.

- the first value in your attribute vector needs to be what will become the ident
- the next value needs to either indicate what valueType you will be using, which is simply the keyword that corresponds with the data type you're assigning OR it can be :enum followed by a vector of enums. While perhapse obvious to those more versed in these things than myself, who's writting this reference, placing the :enum will give you a value type of 'ref' and conveniently enable you to define enums with which to avail yourself of the ref value type

the next ones are optional as cardinality, one of the attributes datomic requires, is simply set to a default of singular absent the presence of the indicator that would indicate otherwise

- the third value, if this is needed, is either :unique-value or :unique-identity depending on which flavor of uniqueness, if any, you are needing to apply to the attribute.

the following values, as best I understand it, don't need to be given in any particular order, the above item on uniqueness might also not require placement as third, but that's how I understood the code. But I'm still green at all this and haven't tested this as of yet.

- :many happens to be what you choose if you don't want your cardinality to default to singular
- :indexed is what to type if you want this attribute indexed
- "" with, or without, a string of documentation inside it, is what you place if you want to document this particular schema attribute
- :fulltext is what you type if you want full text search enabled for this attribute
- :component is what you place if your type is 'ref' and this will be a component (contained by) another attribute in the schema
- :nohistory is what you type if you don't want the history preserved for this attribute
