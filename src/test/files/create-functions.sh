#!/usr/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## Source this file.

EXT="srl"

## setup ()
function clean
{
    rm -f *.srl manifets.ttl
}

## fname ( "basename" Number [extension] )
function fname
{
    local BASE="$1"	# Base
    local N="$2"	# Number
    local EXT="$3"	# Extension
    [ "$EXT" = "" ] && EXT="srl"
    echo $(printf "$BASE%02d.$EXT" $N)
}

## reads from stdin
## testGood filename [Description [fragment]] < body
function testGood
{
    if [ "$#" != 1 ]
    then  
	echo "testGood: Bad arguments: $@"
	exit 1
    fi
    local FN="$1"
    local DESC="$2"
    local FRAG="$3"
    local IDX=${#GOOD[*]}
    GOOD[$IDX]=$FN
    cat > $FN
}

# reads from stdin
## testBad filename < body
function testBad
{
    if [ "$#" != 1 ]
    then  
	echo "testBad: Bad arguments: $@"
	exit 1
    fi
    local FN="$1"
    local DESC="$2"
    local FRAG="$3"
    local IDX=${#BAD[*]}
    BAD[$IDX]=$FN
    cat > $FN
}

function output
{
    local FN="$1"
    local TYPE="$2"
    
    I="$(($I+1))"
    local N=":test_$I"
    local E="\n"
    E="${E}$N rdf:type   $TYPE ;"
    #E="${E}\n   dawgt:approval dawgt:NotClassified ;" ;
    E="${E}\n   mf:name    \"$FN\" ;" 
    E="${E}\n   mf:action  <$FN> ;"
    E="${E}\n   ."

    ## Manifest list items.
    ITEMS="${ITEMS}        $N\n"
    ## Test descriptions
    ENTRIES="${ENTRIES}$E\n"
}
    
function outputLicense
{
    cat <<EOF
## [1] https://www.w3.org/Consortium/Legal/2008/04-testsuite-license
## [2] https://www.w3.org/Consortium/Legal/2008/03-bsd-license

EOF
    
##     cat <<EOF
## #  Licensed to the Apache Software Foundation (ASF) under one or more
## #  contributor license agreements.  See the NOTICE file distributed with
## #  this work for additional information regarding copyright ownership.
## #  The ASF licenses this file to You under the Apache License, Version 2.0
## #  (the "License"); you may not use this file except in compliance with
## #  the License.  You may obtain a copy of the License at
## # 
## #       http://www.apache.org/licenses/LICENSE-2.0
## # 
## #  Unless required by applicable law or agreed to in writing, software
## #  distributed under the License is distributed on an "AS IS" BASIS,
## #  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## #  See the License for the specific language governing permissions and
## #  limitations under the License.
## 
## EOF
}

## createManifest label rootURI
function createManifest
{
    if [ "$#" != 2 ]
    then
	echo "Wrong number of arguments to createManifest" 1>&2
	exit 1
    fi

    local LABEL="$1"
    local URI="$2"
## Header
    (
	outputLicense
	cat <<EOF
PREFIX :       $URI
PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#>
PREFIX mf:     <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>
PREFIX srl:    <http://www.w3.org/ns/shacl-rules#>
PREFIX srt:    <http://www.w3.org/ns/shacl-rules-test#>

<>  rdf:type mf:Manifest ;
    rdfs:comment "$LABEL" ;
    mf:entries (
EOF

    )> manifest.ttl

# Build the manifest list.
# Build the manifest items.

    for f in "${GOOD[@]}"
    do
	output "$f" "${POSTIVE_SYNTAX}"
    done

    for f in "${BAD[@]}"
    do
	output "$f" "${NEGATIVE_SYNTAX}"
    done

    (
	##echo '('
	echo -e -n "$ITEMS"
	echo '    ) .'
	echo -e -n "$ENTRIES"
    )  >> manifest.ttl
}
