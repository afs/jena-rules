#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Stratification

N=0
N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE {?s ?p ?o }
EOF

N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE {:s :p ?o . NOT { :x :data ?z } }
EOF

N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s :p "abc" } WHERE { NOT { ?s :p "XYZ" } SET ( ?s := :sz ) }
RULE { :s :p "ABC" } WHERE { ?s :q :z }
EOF

N=$((N+1)) ; testGood $(fname "stratification-" $N) <<EOF
PREFIX : <http://example/>
RULE { [] :q "Rule" } WHERE { ?s :z ?o }
EOF


## Bad

N=0

N=$((N+1)) ; testBad $(fname "stratification-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s :p "ABC" } WHERE { ?s :data "" . NOT { ?s :p "ABC" } }
EOF

N=$((N+1)) ; testBad $(fname "stratification-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s :p "abc" } WHERE { ?s :data "" . NOT { ?s :p "ABC" } }
RULE { :s :p "ABC" } WHERE { NOT { ?x :p "abc" } ?s :data "" }
EOF

N=$((N+1)) ; testBad $(fname "stratification-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { [] :q "Rule" } WHERE { ?s :q ?o }
EOF

N=$((N+1)) ; testBad $(fname "stratification-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { [] :q ?o } WHERE { ?s :p ?o }
RULE { ?s :p "Rule" } WHERE { ?s ?p "Rule" }
RULE { ?s :q "Rule" } WHERE { ?s :q ?o }
EOF
