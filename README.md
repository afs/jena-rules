# jena-rules

`jena-rule` is a protoype implement of [SHACL Rules](https://www.w3.org/TR/shacl12-rules/)

The intention is to contribute this to [Apache Jena](https://jena.apache.org).

It provides 

* a parser, which can output the rule set is Shapes Rule Language
(SRL) and in RDF.
* an evaluator: the main purose of the evaluator is to track the spec and execute the test cases.


## Parser

```
# Parse and print
rules parse FILE.srl

# Parse and output RDF.
rules parse --output=rdf FILE.srl
```

## Evaluator


```
# Execute the rule set on the data file and output inferred triples and also the combined graph.
rules exec RulesFile DataFile

# Execute the rule set on an empty graph. The rule set may have a DATA clause.
rules exec RulesFile
```

## Build

This requires Java25.

```
mvn clean install
```
