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
    POSTIVE_SYNTAX="srlt:RulesPositiveSyntaxTest"
    NEGATIVE_SYNTAX="srlt:RulesNegativeSyntaxTest"

    setup_dir $DIR
    cd $DIR
    clean
    source $HERE/tests-syntax.sh
    createManifest "SPARQL-RL - Syntax" '<manifest#>'
)

(
    DIR="wellformed"
    POSTIVE_SYNTAX="srlt:RulesPositiveWellFormednessTest"
    NEGATIVE_SYNTAX="srlt:RulesNegativeWellFormednessTest"

    setup_dir $DIR
    cd $DIR
    clean
    source $HERE/tests-wellformed.sh
    createManifest "SPARQL-RL - Well-formedness" '<manifest#>'
)

(
    DIR="stratification"
    POSTIVE_SYNTAX="srlt:RulesPositiveStratificationTest"
    NEGATIVE_SYNTAX="srlt:RulesNegativeStratificationTest"

    setup_dir $DIR
    cd $DIR
    clean
    source $HERE/tests-stratification.sh
    createManifest "SPARQL-RL - Stratification" '<manifest#>'
)

(
    DIR="syntax-jena"
    POSTIVE_SYNTAX="srlt:JenaRulesPositiveSyntaxTest"
    NEGATIVE_SYNTAX="srlt:JenaRulesNegativeSyntaxTest"

    setup_dir $DIR
    cd $DIR
    clean
    source $HERE/tests-syntax-ext.sh
    createManifest "SPARQL-RL - Syntax - Jena" '<manifest#>'
)
