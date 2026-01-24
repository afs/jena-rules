#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## ## Well-formed
## 
## N=0
## 
## N=$((N+1)) ; testGood $(fname "well-formed-rule-" $N) <<EOF
## PREFIX : <http://example>
## RULE { ?s ?p ?o }
## WHERE {
##     ?s ?p ?o . FILTER(?o < 50)
## }
## EOF
## 
## N=$((N+1)) ; testGood $(fname "well-formed-rule-" $N) <<EOF
## PREFIX : <http://example>
## RULE { ?s ?p ?o }
## WHERE {
##     BIND(123 AS ?o)
##     ?s ?p ?o
## }
## EOF
