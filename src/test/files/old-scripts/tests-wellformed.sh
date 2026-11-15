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
    SET ( ?p := :p )
}
EOF

N=$((N+1)) ; testGood $(fname "wellformed-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s ?p ?o }
WHERE {
    ?s :p :z .
    NOT { ?s :q ?y }
    ?s  :q ?o
    SET(?p := :p)
}
EOF

## Bad

N=0
N=$((N+1)) ; testBad $(fname "wellformed-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { ?s ?p ?o }
WHERE {
    ?s ?p ?o
    SET(?o := 123)
}
EOF


N=$((N+1)) ; testBad $(fname "wellformed-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { :s :p ?x }
WHERE {
    SET(?x := 1)
    SET(?x := 1)
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
