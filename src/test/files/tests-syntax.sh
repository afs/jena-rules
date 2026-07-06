#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Source this file.
## Assumes syn-functions.sh

## @@Macros in the head


## RDF rule structure

N=0
N=$((N+1)) ; testGood $(fname "syntax-ruleset-structure-" $N) <<EOF
EOF

N=$((N+1)) ; testGood $(fname "syntax-ruleset-structure-" $N) <<EOF
BASE <http://example/base>
PREFIX : <http://example/>
PREFIX ns: <http://example/ns#>
EOF

N=$((N+1)) ; testGood $(fname "syntax-ruleset-structure-" $N) <<EOF
BASE <http://example/base1>
PREFIX : <http://example/>
BASE <http://example/base2>
PREFIX ns: <http://example/ns#>
EOF


N=$((N+1)) ; testGood $(fname "syntax-ruleset-structure-" $N) <<EOF
DATA {}
RULE {} WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-ruleset-structure-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { }

PREFIX ns: <http://example/ns#>

RULE {} WHERE { :x :p ns:o }

EOF

## RDF Terms

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE { :s :p :o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE { ?s ?p ?o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE { ?s a ?T }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE { } WHERE { ?s :p [] }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE { } WHERE { [] :p ?o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE { } WHERE { _:a :p _:b . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

RULE { } WHERE { ?s :p 123e+10 }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { ?s :p 123 }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { ?s :p -123.5e+10 }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { ?s :p <<( :a :b :c )>> }
EOF

## @@ Bug
N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { ?s :p <<( ?a ?b ?c )>> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p "string" }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p "abc"@en }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p "abc"@en--ltr }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p ( 1 2 3 ) }
EOF

## Body
N=0

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p ( 1 2 3 ) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p [ :q :z ] }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p [ :q1 1 ; :q2 2 ] }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p [ a :T ; ?q ?v ] }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { :sx :p1 :o ; :p2 :o2 . :sy :p :o }
EOF

## FILTERs
N=0
N=$((N+1)) ; testGood $(fname "syntax-rule-elements-filter-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { :s :p ?x FILTER( ?x > 0 ) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-filter-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { FILTER( true ) ?s ?p ?o FILTER ( isURI(?s) ) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-filter-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { FILTER( true ) ?s ?p ?o NOT { ?x ?y ?o FILTER ( isURI(?s) ) } }
EOF

## NOT

N=0
N=$((N+1)) ; testGood $(fname "syntax-rule-elements-not-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { :sx :p1 :o NOT { ?a ?b ?c } }
EOF
N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p "abc"@en--ltr } WHERE {}
EOF


N=$((N+1)) ; testGood $(fname "syntax-rule-elements-not-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { NOT { ?a ?b ?c } ?s ?p ?o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-not-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { NOT { :a :b :c1, :c2 , [ ?q ?z ] } ?s ?p ?o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-elements-not-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { NOT { :a :b :c {| :saidBy :person |} } }
EOF

## Reification macros

N=0
N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { ?s :p << :a :b :c >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { << ?a ?b ?c >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { ?s :p << :a :b :c ~:r >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { << ?a ?b ?c ~ ?r>> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { :s :p :o {| :q :r |} . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { :s :p :o ~_:b {| :q :r |} . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { :s :p :o {| :q1 :r1 , :r2 |} . } 
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { :s :p :o {| :q1 :r1 ; :q2 :r2 |} . } 
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { :s :p :o ~:r1 {| :q1 :r1 |} ~_:r2 {| :q2 :r2 |} . }
EOF

## RDF terms - head

N=0
N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p :o } WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p -123.5 } WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p -123.5e-10 } WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p "string" } WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p "abc"@en } WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p "abc"@en--ltr } WHERE {}
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-head-" $N) <<EOF
PREFIX :    <http://example/>

RULE { :s :p "abc"@en--ltr } WHERE { ?s ?p ?o }
EOF

## 

## DATA

N=0
N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p :o . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p :o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s a :T }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA {
   # Symmetric RDF
   123 :q 456 .
}
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p1 "abc", "abc"@en , "abc"@en--ltr . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA {   :s :p1 1, 1.0, 1e0 . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA {   :s :p1 true, false }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA {   :s :p1 "xyz"^^:datatype }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA {   :s :p :o1, :o2 . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p :o {| :q1 "abc", "abc"@en , "abc"@EN-GB , "abc"@en--ltr ; :q2 1, true |} . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p :o ~:r {| :q :z |} . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p :o ~:r1 {| :q1 :z1 |} ~_:B {| :q1 :z1 |} }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :x :p :z ~_:B {| :q _:B |} }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { :a :b :c ~:r . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { << :s :p :o >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { << :s :p :o >> . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { << :a :b :c ~:r >> . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF
PREFIX : <http://example/>
DATA { << :s :p :o >> :q << :s1 :p1 :o1 >> . }
EOF
  
N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF      
PREFIX : <http://example/>
DATA { <<( :s :p :o )>> :q <<( :s1 :p1 :o1 )>> . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-data-" $N) <<EOF      
PREFIX : <http://example/>
DATA { :s :p :o }
DATA { :x :y :z . }
EOF

## Bad DATA

N=0
N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { a :p :o }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p a }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s "literal" :o }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p "abc"@en--LTR }
EOF


N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p ?o }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s ?p :o }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { ?s :p :o }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p :o ~:r {| |} }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p :o ~ ?r {| :p :z |} }
EOF

## Bad syntax - structure

N=0
N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE {}
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
RULE {} WHERE {:s :p :o }
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE {:s [] :o }
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE {?s ?p ?o NOT { ?a ?b ?c { NOT ?d ?e ?f } } }
EOF
