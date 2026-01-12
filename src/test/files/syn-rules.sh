#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Source this file.
## Assumes syn-functions.sh

N=0

N=$((N+1)) ; testGood $(fname "syntax-rule-empty-" $N) <<EOF
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-empty-" $N) <<EOF
PREFIX : <http://example>
EOF

N=0

N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
RULE {} WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX : <http://example>
RULE {} WHERE { :s :p :o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :q :z } WHERE { ?s :p :o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX :    <http://example>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

RULE { ?s :q :z } WHERE { ?s :p "123"^^xsd:xsd:nonNegativeInteger }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :q :z ; :q ?r } WHERE { ?s :p :o ; :q ?r }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :q :z ; :q ?r }
WHERE {
    ?s :p1 [ :q1 123 ] ;
}
EOF
N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :q :z ; :q ?r }
WHERE {
    ?s :p2 ( 1 2 ?x 3 ) ;
}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :q :z ; :q ?r }
WHERE {
    ?s :p3 <<( :s :p :o )>> ;
}
EOF
N=$((N+1)) ; testGood $(fname "syntax-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s :q :z ; :q ?r }
WHERE {
    ?s :p4 << :s :p :o >> ;
}
EOF

## Well-formed

N=0

N=$((N+1)) ; testGood $(fname "well-formed-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s ?p ?o }
WHERE {
    ?s ?p ?o . FILTER(?o < 50)
}
EOF

N=$((N+1)) ; testGood $(fname "well-formed-rule-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s ?p ?o }
WHERE {
    BIND(123 AS ?o)
    ?s ?p ?o
}
EOF



## Bad syntax

N=0

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example>
RULE
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example>
RULE {}
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example>
RULE {} WHERE
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
RULE {} WHERE {:s :p :o }
EOF




## Bad Well-formedness

N=0

N=$((N+1)) ; testGood $(fname "well-formed-rule-bad-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s ?p ?o }
WHERE {
    ?s ?p ?o
    BIND(123 AS ?o)
}
EOF

N=$((N+1)) ; testGood $(fname "well-formed-rule-bad-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s ?p ?o }
WHERE {
    FILTER(?o < 50)
    ?s ?p ?o
}
EOF
