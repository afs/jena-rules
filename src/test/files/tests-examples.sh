#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Run this file.
## Does not use create-functions.sh

#cd examples

N=0

N=$((N+1))
# Example 1
(
    X=$(printf "%02d" $N)
    X=$N
    INPUT="example-$X-data.ttl"
    RULES="example-$X.srl"
    INF="example-$X-inf.ttl"

    cat > $INPUT <<EOF
PREFIX : <http://example/>

:frontend :callsService :app .
:app      :queriesDatabase :db .
:db       :hasVulnerability :vuln1 .
EOF

    cat > $RULES <<EOF
PREFIX : <http://example/>

RULE { ?x :dependsOn ?y } WHERE { ?x :callsService ?y }
RULE { ?x :dependsOn ?y } WHERE { ?x :queriesDatabase ?y }
EOF

    cat > $INF <<EOF
PREFIX : <http://example/>

:frontend :dependsOn :app .
:app      :dependsOn :db .
EOF
)

N=$((N+1))
# Example 2
(
    X=$(printf "%02d" $N)
    X=$N
    INPUT="example-$X-data.ttl"
    RULES="example-$X.srl"
    INF="example-$X-inf.ttl"
    
    cat > $INPUT <<EOF
PREFIX : <http://example/>

:frontend :callsService :app .
:app      :queriesDatabase :db .
:db       :hasVulnerability :vuln1 .
EOF

    cat > $RULES <<EOF
PREFIX : <http://example/>

RULE { ?x :dependsOn ?y } WHERE { ?x :callsService ?y }
RULE { ?x :dependsOn ?y } WHERE { ?x :queriesDatabase ?y }
RULE { ?x :exposedTo ?v } WHERE { ?x :dependsOn ?y . ?y :hasVulnerability ?v }
EOF

    cat > $INF <<EOF
PREFIX : <http://example/>

:app      :exposedTo :vuln1 .
:frontend :dependsOn :app .
:app      :dependsOn :db .
EOF
)

N=$((N+1))
# Example 3
(
    X=$(printf "%02d" $N)
    X=$N
    INPUT="example-$X-data.ttl"
    RULES="example-$X.srl"
    INF="example-$X-inf.ttl"  
    
    cat > $INPUT <<EOF
PREFIX : <http://example/>

:frontend :callsService :app .
:app      :queriesDatabase :db .
:db       :hasVulnerability :vuln1 .
EOF

    cat > $RULES <<EOF
PREFIX : <http://example/>

RULE { ?x :dependsOn ?y } WHERE { ?x :callsService ?y }
RULE { ?x :dependsOn ?y } WHERE { ?x :queriesDatabase ?y }
RULE { ?x :exposedTo ?v } WHERE { ?x :hasVulnerability ?v }
RULE { ?x :exposedTo ?v } WHERE { ?x :dependsOn ?y . ?y :exposedTo ?v }
EOF

    cat > $INF <<EOF
PREFIX : <http://example/>

:db       :exposedTo :vuln1 .
:app      :exposedTo :vuln1 .
:frontend :exposedTo :vuln1 .
:frontend :dependsOn :app .
:app      :dependsOn :db .
EOF
)

N=$((N+1))
# Example 4
(
    X=$(printf "%02d" $N)
    X=$N
    INPUT="example-$X-data.ttl"
    RULES="example-$X.srl"
    INF="example-$X-inf.ttl"  
    
    cat > $INPUT <<EOF
PREFIX : <http://example/>

:app1 :exposedTo :vuln1 .
:app2 :exposedTo :vuln2 .
:vuln1 :severity 9.1 .
:vuln2 :severity 4.3 .
EOF

    cat > $RULES <<EOF
PREFIX : <http://example/>

RULE { ?x :status :criticallyExposed }
WHERE {
    ?x :exposedTo ?v .
    ?v :severity ?s .
    FILTER(?s >= 9.0)
}
EOF

    cat > $INF <<EOF
PREFIX : <http://example/>

:app1 :status :criticallyExposed .
EOF
)

N=$((N+1))
# Example 5
(
    X=$(printf "%02d" $N)
    X=$N
    INPUT="example-$X-data.ttl"
    RULES="example-$X.srl"
    INF="example-$X-inf.ttl"  
    
    cat > $INPUT <<EOF
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX :     <http://example/>

:app rdf:type :Component ;
:status :criticallyExposed .
:logger rdf:type :Component .
EOF

    cat > $RULES <<EOF
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX :     <http://example/>

RULE { ?x :status :safeToDeploy }
WHERE {
    ?x rdf:type :Component .
    NOT { ?x :status :criticallyExposed }
}
EOF

    cat > $INF <<EOF
PREFIX : <http://example/>

:logger :status :safeToDeploy .
EOF
)

## -------------------------------------

cat > manifest.ttl <<EOF
## [1] https://www.w3.org/Consortium/Legal/2008/04-testsuite-license
## [2] https://www.w3.org/Consortium/Legal/2008/03-bsd-license

PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> 
PREFIX mf:     <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> 

PREFIX :       <https://w3c.github.io/rdf-tests/shacl/shacl12/>

PREFIX srt:    <http://www.w3.org/ns/shacl-rules-test#>

<#>  rdf:type mf:Manifest ;
   rdfs:label "SRL Examples"@en ;
   mf:assumedTestBase <https://w3c.github.io/rdf-tests/shacl/shacl12/> ;
    mf:entries
    (
      :example-1
      :example-2
      :example-3
      :example-4
      :example-5
    ) .
EOF

for X in 1 2 3 4 5
do
    cat >> manifest.ttl<<EOF

:example-$X rdf:type srt:RulesEvalTest ;
    mf:name "Example $X" ;
    mf:action
         [ srt:ruleset  <example-$X.srl> ;
           srt:data     <example-$X-data.ttl> ] ;
    mf:result  <example-$X-inf.ttl> ;
    .
EOF
done
