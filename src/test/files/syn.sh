#!/bin/bash
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0
##

source syn-functions.sh
HERE="$PWD"

DIR="syntax"

if [ -e "$DIR" ]
then
    rm -rf "$DIR"
fi
mkdir -p "$DIR"

(
    cd $DIR
    clean
    source $HERE/syn-rules.sh
    createManifest "SHACL Rules - Syntax" '<manifest#>'
)
