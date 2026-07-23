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

import org.junit.jupiter.api.Test;

// Tests due to go to the spec, currently in Java.s
public class TestRulesEval_Spec {
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
        LibEvalTest.testEval("assign2", baseGraph, rules, expectedInf);
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
        LibEvalTest.testEval("set1", baseGraph, rules, expectedInf);
    }
}
