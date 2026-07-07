#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Source this file.
## Assumes create-functions.sh

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

## Make this all concrete rdf terms
## Remove from *data and *template

## Covered in-context below.
## RDF terms

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE { :s :p :o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE { } WHERE { ?s :p [] }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-terms-" $N) <<EOF
PREFIX : <http://example/>
RULE { } WHERE { ?s :p _:b . }
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

## Decimals.
## IRIs


## Bad RDF terms

N=0
N=$((N+1)) ; testBad $(fname "syntax-rule-terms-bad-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { :s :p "abc"@en--LTR }
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-terms-bad-" $N) <<EOF
PREFIX :    <http://example/>
## Blank node labels don't end in . so it becomes a DOT
RULE { } WHERE { :s :p _:label. ; :q "" }
EOF

N=$((N+1)) ; testBad $(fname "syntax-rule-terms-bad-" $N) <<EOF
PREFIX :    <http://example/>
## Decimals do not end in . so it becomes a DOT
RULE { } WHERE { :s :p 123. ; :q "" }
EOF

## Paths
N=0
N=$((N+1)) ; testGood $(fname "syntax-rule-paths-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { :sx :p1/:p2 :o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-paths-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { ?x ^:p1/^:p2 ?o }
EOF

N=$((N+1)) ; testGood $(fname "syntax-rule-paths-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { ?x :p1/a ?o }
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

N=0
N=$((N+1)) ; testGood $(fname "syntax-rule-elements-not-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { :sx :p1 :o NOT { ?a ?b ?c } }
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
## There is also copverage in -data-, -template- and -pattern-
## As this synatx is new, provide some focused test as terms.

N=0
N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { ?s :p << :a :b :c >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { << :a :b :c >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

RULE { } WHERE { ?s :p << :a :b :c ~:r >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p :o {| :q :r |} . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p :o ~_:b {| :q :r |} . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p :o {| :q1 :r1 , :r2 |} . } 
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p :o {| :q1 :r1 ; :q2 :r2 |} . } 
EOF

N=$((N+1)) ; testGood $(fname "syntax-reification-" $N) <<EOF
PREFIX :    <http://example/>

RULE { } WHERE { :s :p :o ~:r1 {| :q1 :r1 |} ~_:r2 {| :q2 :r2 |} . }
EOF

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

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p "xyx"^^:datatype. . }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p 1. . }
EOF

N=$((N+1)) ; testBad $(fname "syntax-data-bad-" $N) <<EOF
PREFIX : <http://example/>
DATA { :s :p _:b. . }
EOF

## Head templates

N=0
N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a ?b ?c } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a a :T } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a ?b :c1 , :c2 } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a ?b  "abc", "abc"@en , "abc"@en--ltr . } WHERE { ?a ?b ?c }
EOF


N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a :p1 1, 1.0, 1e0 . } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a :p1 true, false . } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a :p  "xyz"^^:datatype } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { :s :p :o {| ?b ?c , "abc", "abc"@en , "abc"@en--ltr ; :q2 1, true |} } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { :s :p :o ~:r {| :q :z |} } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { :s :p ?a ~ ?c1 {| ?b ?c2 |} } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { :s :p :o ~:r1 {| :q1 :z1 |} ~_:B {| :q1 :z1 |} } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { :x :p :z ~_:B {| :q _:B |} } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { << ?a ?b ?c >> } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { << ?a ?b ?c ~:r >> } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { << :s :p :o >> :q << ?a ?b ?c  >> } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { <<( ?a ?b :o )>> :q <<( :s :p ?c )>> } WHERE { ?a ?b ?c }
EOF


N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a :p () } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a ?b (1) } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { ?a ?b (?c) } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { :x :q ("a" :x true) } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { () :q :x } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { (2) :q :x } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { (1 2 3) :p :z } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { [ ?b ?c ] } WHERE { ?a ?b ?c  }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { [ ?b ?c ; :p :z ] } WHERE { ?a ?b ?c  }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { [ ?b ?c ; :p :z ] :q :z } WHERE { ?a ?b ?c  }
EOF

N=$((N+1)) ; testGood $(fname "syntax-template-" $N) <<EOF
PREFIX : <http://example/>

RULE { [ ?b ?c ; :p :z ] :q [] } WHERE { ?a ?b ?c  }
EOF

## Bad templates

N=0
N=$((N+1)) ; testBad $(fname "syntax-template-bad-" $N) <<EOF
PREFIX :    <http://example/>
RULE { :s :p "abc"@en--LTR } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testBad $(fname "syntax-template-bad-" $N) <<EOF
PREFIX :    <http://example/>
RULE { <iri with space> :p "abc" } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testBad $(fname "syntax-template-bad-" $N) <<EOF
PREFIX :    <http://example/>
RULE { a :p "abc" } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testBad $(fname "syntax-template-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE { :s :p :o ~:r {| |} } WHERE { ?a ?b ?c }
EOF


## Body Patterns

N=0
N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE { } WHERE { ?a ?b ?c }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a a :T }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a ?b :c1 , :c2 }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a ?b  "abc", "abc"@en , "abc"@en--ltr . }
EOF


N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a :p1 1, 1.0, 1e0 . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a :p1 true, false . }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a :p  "xyz"^^:datatype }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { :s :p :o {| ?b ?c , "abc", "abc"@en , "abc"@en--ltr ; :q2 1, true |} }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { :s :p :o ~:r {| :q :z |} }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { :s :p ?a ~ ?c1 {| ?b ?c2 |} }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { :s :p :o ~:r1 {| :q1 :z1 |} ~_:B {| :q1 :z1 |} }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { :x :p :z ~_:B {| :q _:B |} }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { << ?a ?b ?c >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { << ?a ?b ?c ~:r >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { << :s :p :o >> :q << ?a ?b ?c  >> }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { <<( ?a ?b :o )>> :q <<( :s :p ?c )>> }
EOF


N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a :p () }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a ?b (1) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { ?a ?b (?c) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { :x :q ("a" :x true) }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { () :q :x }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { (2) :q :x }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { (1 2 3) :p :z }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { [ ?b ?c ] }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { [ ?b ?c ; :p :z ] }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { [ ?b ?c ; :p :z ] :q :z }
EOF

N=$((N+1)) ; testGood $(fname "syntax-pattern-" $N) <<EOF
PREFIX : <http://example/>

RULE {} WHERE { [ ?b ?c ; :p :z ] :q [] }
EOF

## Bad patterns

N=0
N=$((N+1)) ; testBad $(fname "syntax-pattern-bad-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { :s :p "abc"@en--LTR }
EOF

N=$((N+1)) ; testBad $(fname "syntax-pattern-bad-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { <iri with space> :p "abc" }
EOF

N=$((N+1)) ; testBad $(fname "syntax-pattern-bad-" $N) <<EOF
PREFIX :    <http://example/>
RULE { } WHERE { a :p "abc" }
EOF

N=$((N+1)) ; testBad $(fname "syntax-pattern-bad-" $N) <<EOF
PREFIX : <http://example/>
RULE {} WHERE { :s :p :o ~:r {| |} }
EOF
