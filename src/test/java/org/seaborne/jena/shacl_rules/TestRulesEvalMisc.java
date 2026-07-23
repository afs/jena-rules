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


//"Interesting" tests
public class TestRulesEvalMisc {
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

     // A path where the step is more complex than a SPARQL property path.
     // conditionalPathClosure1 -- each side of the weak connected of length 1 -- less inferred triples.
     // conditionalPathClosure2 -- each side of the weak connected of length 2

     @Test public void conditionalPathClosure1() {

         String baseGraph = """
                 PREFIX : <http://example/>

                 ##:x0 :link :x1 ~:linkA {| :weight 9 |} .
                 :x1 :link :x2 ~:linkB {| :weight 9 |} .

                 # Weak link: :x2 to :x3
                 :x2 :link :x3 ~:linkC {| :weight 1 |} .

                 :x3 :link :x4 ~:linkD {| :weight 8 |} .
                 ##:x4 :link :x5 ~:linkE {| :weight 8 |} .
                 """;
         String rules = """
                 PREFIX : <http://example/>

                 ## Path where a "connection" must be weight > 5

                 ## Condition for one step in the path.
                 RULE { ?x :connected ?z } WHERE {
                      ?x :link ?z ~?link {| :weight ?w |}
                      FILTER(?w > 5)
                 }

                 ## Recursion "connected"
                 RULE { ?x :connected ?y } WHERE {
                     ?x :connected ?z . ?z :connected ?y
                 }
                 """;
         String expectedInf = """
                 PREFIX : <http://example/>
                 ## no :connected through :x2-x3
                 :x3 :connected :x4 .
                 :x1 :connected :x2 .
                 """;
         LibEvalTest.testEval("assign1", baseGraph, rules, expectedInf);
     }

     @Test public void conditionalPathClosure2() {

         String baseGraph = """
                 PREFIX : <http://example/>

                 :x0 :link :x1 ~:linkA {| :weight 9 |} .
                 :x1 :link :x2 ~:linkB {| :weight 9 |} .

                 # Weak link: :x2 to :x3
                 :x2 :link :x3 ~:linkC {| :weight 1 |} .

                 :x3 :link :x4 ~:linkD {| :weight 8 |} .
                 :x4 :link :x5 ~:linkE {| :weight 8 |} .
                 """;
         String rules = """
                 PREFIX : <http://example/>

                 ## Path where a "connection" must be weight > 5

                 ## Condition for one step in the path.
                 RULE { ?x :connected ?z } WHERE {
                      ?x :link ?z ~?link {| :weight ?w |}
                      FILTER(?w > 5)
                 }

                 ## Recursion "connected"
                 RULE { ?x :connected ?y } WHERE {
                     ?x :connected ?z . ?z :connected ?y
                 }
                 """;
         String expectedInf = """
                 PREFIX : <http://example/>
                 ## no :connected through :x2-x3
                 :x4 :connected :x5 .
                 :x3 :connected :x5 .
                 :x3 :connected :x4 .
                 :x1 :connected :x2 .
                 :x0 :connected :x2 .
                 :x0 :connected :x1 .
                 """;
         LibEvalTest.testEval("assign1", baseGraph, rules, expectedInf);
     }

 }
