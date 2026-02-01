#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Well-formed

N=0

N=$((N+1)) ; testGood $(fname "wellformed-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s ?p ?o }
WHERE {
    ?s ?p ?o
}
EOF

N=$((N+1)) ; testGood $(fname "wellformed-" $N) <<EOF
PREFIX : <http://example>
RULE { ?s ?p ?o }
WHERE {
    ?s ?p ?o . FILTER(?o < 50)
}
EOF


N=$((N+1)) ; testGood $(fname "wellformed-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s ?p ?o }
WHERE {
    ?s :p ?o .
    BIND ( :p AS ?p  )
}
EOF

N=$((N+1)) ; testGood $(fname "wellformed-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s ?p ?o }
WHERE {
    ?s :p :z .
    NOT { ?s :q ?y }
    ?s  :q ?o
    BIND(:p AS ?p)
}
EOF

## Bad

N=0
N=$((N+1)) ; testBad $(fname "wellformed-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s ?p ?o }
WHERE {
    ?s ?p ?o
    BIND(123 AS ?o)
}
EOF


N=$((N+1)) ; testBad $(fname "wellformed-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { :s :p ?x }
WHERE {
    BIND(1 AS ?x)
    BIND(1 AS ?x)
}
EOF

N=$((N+1)) ; testBad $(fname "wellformed-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s ?p ?o }
WHERE {
    FILTER(?o < 50)
    ?s ?p ?o
}
EOF

N=$((N+1)) ; testBad $(fname "wellformed-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s ?p ?o }
WHERE {
  ?s ?p ?z
}
EOF
