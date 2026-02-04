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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.*;
import org.apache.jena.sparql.graph.GraphZero;
import org.apache.jena.sparql.util.IsoMatcher;
import org.seaborne.jena.shacl_rules.exec.EngineType;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;

/**
 * Java-written evaluation tests.
 * Most testing is in scripts.
 * @see Scripts_RuleEval
 */
public class TestRulesEval {
    @Test public void eval_1() {
        Graph data = graph();
        RuleSet rules = rules("RULE {} WHERE {}");
        Graph outcome = graph();
        test("eval_1", data, rules, outcome);
    }

    @Test public void eval_2() {
        Graph data = graph();
        RuleSet rules = rules("DATA { :s :p :o }");
        Graph outcome = graph(":s :p :o .");
        test("eval_2", data, rules, outcome);
    }

    // Check NOW is stable.
    @Test public void eval_now() {
        Graph data = graph();
        RuleSet rules = rules("""
                RULE { :x :p1 ?now1 } WHERE { BIND (NOW() AS ?now1) }
                RULE { :x :p2 ?now2 } WHERE { BIND (NOW() AS ?now2) }
                RULE { :x :result true } WHERE { :x :p1 ?now1 . :x :p2 ?now2. FILTER(?now1 = ?now2) }
                RULE { :x :result false } WHERE { :x :p1 ?now1 . :x :p2 ?now2. FILTER(?now1 != ?now2) }
                """);
        Graph outcome = graph(":x :result true.");
        testContains("eval_2", data, rules, outcome);
    }

    // ---- Machinery

    private void test(String label, Graph data, RuleSet ruleSet, Graph expectedOutcome) {
        Graph actualOutcome = testEval(label, data, ruleSet).outputGraph() ;
        boolean pass = IsoMatcher.isomorphic(expectedOutcome, actualOutcome);
        if (! pass ) {
            printFailedEvalTest(label, data, ruleSet, expectedOutcome, actualOutcome);
            fail(label+" : Results do not match");
        }
    }

    private RuleSetEvaluation testEval(String label, Graph data, RuleSet ruleSet) {
        EngineType engineType = EngineType.SIMPLE;
        RuleSetEvaluation e = RulesEngine.create(engineType, data, ruleSet).setTrace(false).eval();
        return e;
    }

    private void testContains(String label, Graph data, RuleSet ruleSet, Graph expectedOutcome) {
        Graph actualOutcome = testEval(label, data, ruleSet).outputGraph();
        boolean pass = expectedOutcome.stream().allMatch(t->actualOutcome.contains(t));
        if (! pass ) {
            printFailedEvalTest(label, data, ruleSet, expectedOutcome, actualOutcome);
            fail(label+" : Results not contained in the results");
        }
    }


    private static void printFailedEvalTest(String label, Graph data, RuleSet ruleSet, Graph expectedOutcome, Graph actualOutcome) {
        PrintStream out = System.out;
        out.println("=======================================");
        out.println("---- Failure: " + label);
        out.println("---- Actual:");
        write(out, actualOutcome);
        out.println("---------------------------------------");
        out.println("---- Expected:");
        write(out, expectedOutcome);
        out.println("---------------------------------------");
    }

    private static void write(OutputStream out, Graph graph) {
        RDFWriter.source(graph).format(RDFFormat.TURTLE_FLAT).output(out);
    }

    private static final String PREFIXES_RDF = """
            PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>
            """;
    private static final String PREFIXES_DATA = """
            PREFIX :   <http://example/>
            """;
    private static final String PREFIXES_SHACL = """
            PREFIX sh:      <http://www.w3.org/ns/shacl#>
            PREFIX srl:     <http://www.w3.org/ns/shacl-rules#>
            PREFIX shnex:   <http://www.w3.org/ns/shacl-node-expr#>
            PREFIX sparql:  <http://www.w3.org/ns/sparql#>
            PREFIX arqnx:   <http://jena.apache.org/ARQ/nx#>
            """;

    private RuleSet rules(String string) {
        String ruleStr = PREFIXES_SHACL+PREFIXES_DATA+string;
        try {
            return ShaclRulesParser.fromString(ruleStr).parse();
        } catch (ShaclRulesParseException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }

    private Graph graph() { return GraphZero.instance(); }

    private Graph graph(String string) {

        String dataStr = PREFIXES_RDF+PREFIXES_DATA+string;
        try {
            return RDFParser.fromString(dataStr, Lang.TTL).toGraph();
        } catch (RiotException ex) {
                System.err.println(ex.getMessage());
            throw ex;
        }
    }
}
