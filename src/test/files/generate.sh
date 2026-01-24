#!/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0
##

source create-functions.sh
HERE="$PWD"

function setup_dir {
    local DIR="$1"
    if [ -e "$DIR" ]
    then
	rm -rf "$DIR"
    fi
    mkdir -p "$DIR"
}
    
(
    DIR="syntax"
    POSTIVE_SYNTAX="srt:RulesPositiveSyntaxTest"
    NEGATIVE_SYNTAX="srt:RulesNegativeSyntaxTest"

    setup_dir $DIR
    cd $DIR
    clean
    source $HERE/tests-syntax.sh
    createManifest "SHACL Rules - Syntax" '<manifest#>'
)

(
    DIR="wellformed"
    POSTIVE_SYNTAX="srt:RulesPositiveWellFormednessTest"
    NEGATIVE_SYNTAX="srt:RulesNegativeWellFormednessTest"

    setup_dir $DIR
    cd $DIR
    clean
    source $HERE/tests-wellformed.sh
    createManifest "SHACL Rules - Well-formedness" '<manifest#>'
)

(
    DIR="stratification"
    POSTIVE_SYNTAX="srt:RulesPositiveStratificationTest"
    NEGATIVE_SYNTAX="srt:RulesNegativeStratificationTest"

    setup_dir $DIR
    cd $DIR
    clean
    source $HERE/tests-stratification.sh
    createManifest "SHACL Rules - Stratification" '<manifest#>'
)
