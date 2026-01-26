#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Stratification

N=0
N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example>
RULE {} WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example>
RULE {} WHERE {?s ?p ?o }
EOF

N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example>
RULE {} WHERE {:s :p ?o . NOT { :x :data ?z } }
EOF

N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :p "abc" } WHERE { NOT { ?s :p "XYZ" } BIND ( :sz AS ?s ) }
RULE { :s :p "ABC" } WHERE { ?s :q :z }
EOF

## Bad

N=0
N=$((N+1)) ; testBad $(fname "stratification-bad-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :p "abc" } WHERE { NOT { ?s :p "ABC" } ?s :data "" }
RULE { :s :p "ABC" } WHERE { NOT { ?x :p "abc" } ?s :data "" }

EOF
