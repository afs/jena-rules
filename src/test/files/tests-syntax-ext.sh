#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Source this file.
## Assumes syn-functions.sh

## @@Macros in the head

## Jena extensions

## Tuples

N=0
N=$((N+1)) ; testGood $(fname "syntax-ext-tuples-" $N) <<EOF
RULE {} WHERE { tuple(?s, ?p, ?o) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-ext-tuples-" $N) <<EOF
RULE {} WHERE { \$(?s, ?p, ?o) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-ext-tuples-" $N) <<EOF
RULE { TUPLE("num", 123)  } WHERE { }
EOF

N=$((N+1)) ; testGood $(fname "syntax-ext-tuples-" $N) <<EOF
RULE { \$("num", 123) } WHERE { }
EOF


## Bad syntax

N=0

N=$((N+1)) ; testBad $(fname "syntax-ext-tuples-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { } WHERE { TUPLE(TUPLE()) }
EOF
