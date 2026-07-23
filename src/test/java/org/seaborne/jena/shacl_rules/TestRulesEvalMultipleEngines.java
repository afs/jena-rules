/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 */

package org.seaborne.jena.shacl_rules;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.seaborne.jena.shacl_rules.exec.EngineType;

@ParameterizedClass(name="{index}: {0}")
@MethodSource("provideArgs")

public class TestRulesEvalMultipleEngines {

    // XXX Convert to cross product of engine type and manifest!
    // Or find base in-java tests

    // Easier : In Scripts_RuleEval, write up the engine type.

    private static Stream<Arguments> provideArgs() {
        List<Arguments> x = List.of(Arguments.of("Simple (Java)", EngineType.SIMPLE),
                                    Arguments.of("Simple (SPARQL)", EngineType.SIMPLE_SPARQL),
                                    Arguments.of("Simple (CONSTRUCT)", EngineType.SIMPLE_SPARQL_INSERT),
                                    Arguments.of("Simple (INSERT)", EngineType.SIMPLE_SPARQL_CONSTRUCT));
        return x.stream();
    }

    EngineType engineType;

    public TestRulesEvalMultipleEngines(String name, EngineType engineType) {
        this.engineType = engineType;
    }

    //Find/add basic tests.

    // Like RuleEvalTest but in Java.
    // XXX Align with RuleEvalTest

    // XXX Merge/check with TestRulesEval
    // Extract the machinery so we can have several TestXXX


    // tests SET
    // [ ] RULE { :x :q 999 } WHERE { SET(?o1 := 1/0) }
    // ==== TESTS
    // [ ] RunOnce!
    // [ ] Syntax tests
    // [ ] Eval tests
    //     RDFS
    //     link-lorry
    //  [ ] For SET(?x) is BIND(?x) FILTER(BOUND(?x))
    //  [ ] ?x in the head.
    //  [ ] ?x not in the head, used in pattern.


    /*
     * ## Test SET filters the whole row
     * DATA { :s :p 1 . :s :q 1 .  }
     * RULE { :s ?p ?x . . }
     * WHERE {
     *   SET(?x := 1/0) :s ?p ?x
     * }
     */

    static String PREFIXES = """
            PREFIX :        <http://example/>
            """;
    static String PREFIXES_RDF = """
            PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
            """;

    static String PREFIXES_XSD = """
            PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>
            """;


    @Test public void assign1() {
        String rules = PREFIXES+"""
                RULE { :s :p ?v } WHERE { SET( ?v := 1 ) }
                """;
        String expectedInf = PREFIXES+"""
                :s :p 1 .
                """;
        LibEvalTest.testEval("assign1", null, rules, expectedInf);
    }

    @Test public void assign2() {
        String baseGraph = PREFIXES+"""
                :x :xv 1
                """;
        String rules = PREFIXES+"""
                RULE { :s :p ?v1 } WHERE { :x :xv ?v SET( ?v1 := ?v + 1 ) }
                """;
        String expectedInf = PREFIXES+"""
                :s :p 2 .
                """;
        LibEvalTest.testEval("assign2", engineType, baseGraph, rules, expectedInf);
    }

    @Test public void assign3() {
        String baseGraph = PREFIXES+"""
                :s :p 1 .
                """;
        String rules = PREFIXES+"""
                ## Test SET filters the whole row
                RULE { :Z ?p :z  . } WHERE {
                     SET(?x := 1/0)
                     :s ?p ?x
                     }
                """;
        String expectedInf = PREFIXES+"""
                ## And not
                ##   :Z :p :z .
                """;
        LibEvalTest.testEval("set1", engineType, baseGraph, rules, expectedInf);
    }

    @Test public void rdfsDomain1() {
        String baseGraph = PREFIXES_RDF+PREFIXES+"""
                :s :p :z .
                :p rdfs:domain :D .
                :p rdfs:range :R .
                """;
        String rules = PREFIXES_RDF+PREFIXES+"""
                RULE { ?s rdf:type ?T }
                WHERE { ?s ?p ?o . ?p rdfs:domain ?T . }
                """;
        String expectedInf = PREFIXES+"""
                :s a :D .
                """;
        LibEvalTest.testEval("rdfsDomain1", engineType, baseGraph, rules, expectedInf);
    }

    @Test public void rdfsDomain2() {
        String baseGraph = PREFIXES_RDF+PREFIXES+"""
                :s :p :z1 .
                :s :q :z2 .
                :p rdfs:domain :D1 .
                :q rdfs:domain :D2 .
                """;
        String rules = PREFIXES_RDF+PREFIXES+"""
                RULE { ?s rdf:type ?T }
                WHERE { ?s ?p ?o . ?p rdfs:domain ?T . }
                """;
        String expectedInf = PREFIXES+"""
                :s a :D1 .
                :s a :D2 .
                """;
        LibEvalTest.testEval("rdfsDomain1", baseGraph, rules, expectedInf);
    }

    @Test public void rdfsDomain3() {
        String baseGraph = PREFIXES_RDF+PREFIXES+"""
                :s :q :z .
                :p rdfs:domain :D .
                """;
        String rules = PREFIXES_RDF+PREFIXES+"""
                RULE { ?s rdf:type ?T }
                WHERE { ?s ?p ?o . ?p rdfs:domain ?T . }
                """;
        String expectedInf = PREFIXES+"""
                # Empty
                """;
        LibEvalTest.testEval("rdfsDomain1", engineType, baseGraph, rules, expectedInf);
    }

    @Test public void rdfsRange1() {
        String baseGraph = PREFIXES_RDF+PREFIXES+"""
                :s :p :z .
                :p rdfs:domain :D .
                :p rdfs:range :R .
                """;
        String rules = PREFIXES_RDF+PREFIXES+"""
                RULE { ?o rdf:type ?T }
                WHERE { ?s ?p ?o . ?p rdfs:range ?T . }
                """;
        String expectedInf = PREFIXES+"""
                :z a :R .
                """;
        LibEvalTest.testEval("rdfsRange1", engineType, baseGraph, rules, expectedInf);
    }

    @Test public void rdfsRange2() {
        String baseGraph = PREFIXES_RDF+PREFIXES+"""
                :s :p :z1 .
                :s :q :z2 .
                :p rdfs:range :R1 .
                :q rdfs:range :R2 .
                """;
        String rules = PREFIXES_RDF+PREFIXES+"""
                RULE { ?o rdf:type ?T }
                WHERE { ?s ?p ?o . ?p rdfs:range ?T . }
                """;
        String expectedInf = PREFIXES+"""
                :z1 a :R1 .
                :z2 a :R2 .
                """;
        LibEvalTest.testEval("rdfsRange1", engineType, baseGraph, rules, expectedInf);
    }

    @Test public void rdfsRange3() {
        String baseGraph = PREFIXES_RDF+PREFIXES+"""
                :s :q :z .
                :p rdfs:range :R .
                """;
        String rules = PREFIXES_RDF+PREFIXES+"""
                RULE { ?o rdf:type ?T }
                WHERE { ?s ?p ?o . ?p rdfs:range ?T . }
                """;
        String expectedInf = PREFIXES+"""
                # Empty
                """;
        LibEvalTest.testEval("rdfsRange1", engineType, baseGraph, rules, expectedInf);
    }
}
