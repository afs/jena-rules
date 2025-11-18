#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Source this file.
## Assumes syn-functions.sh

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




## Bad syntax - including well-frmness failures.

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

## Well-formedness
