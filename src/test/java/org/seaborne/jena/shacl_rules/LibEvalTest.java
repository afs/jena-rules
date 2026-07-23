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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.IsoMatcher;
import org.seaborne.jena.shacl_rules.exec.EngineType;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.sys.SysJenaRules;

public class LibEvalTest {

    static void testEval(String label, String baseGraphStr, String rulesStr, String expectedInfStr) {
        Graph baseGraph = (baseGraphStr == null)
                ? GraphFactory.emptyGraph()
                : RDFParser.fromString(baseGraphStr, Lang.TURTLE).toGraph();
        Graph expectedInf = RDFParser.fromString(expectedInfStr, Lang.TURTLE).toGraph();
        RuleSet ruleSet = ShaclRulesParser.parseString(rulesStr);

//        printTest(label, baseGraphStr, rulesStr, expectedInfStr);

        testEval(SysJenaRules.dftEngineType, baseGraph, ruleSet, expectedInf);
    }

    static void testEval(String label, EngineType engineType, String baseGraphStr, String rulesStr, String expectedInfStr) {
        Graph baseGraph = (baseGraphStr == null)
                ? GraphFactory.emptyGraph()
                : RDFParser.fromString(baseGraphStr, Lang.TURTLE).toGraph();
        Graph expectedInf = RDFParser.fromString(expectedInfStr, Lang.TURTLE).toGraph();
        RuleSet ruleSet = ShaclRulesParser.parseString(rulesStr);

//        printTest(label, baseGraphStr, rulesStr, expectedInfStr);

        testEval(engineType, baseGraph, ruleSet, expectedInf);

    }

    static void printTest(String label, String baseGraph, String ruleSet, String expectedInf) {
        if ( baseGraph == null )
            baseGraph = "Empty\n";
        System.out.println("== == "+label+" == ==");
        System.out.print(baseGraph);
        System.out.println("--");
        System.out.print(ruleSet);
        System.out.println("--");
        System.out.print(expectedInf);
    }


    static void testEval(EngineType engineType, Graph baseGraph, RuleSet ruleSet, Graph expectedInfGraph) {
        Objects.requireNonNull(baseGraph);
        Objects.requireNonNull(ruleSet);
        Objects.requireNonNull(expectedInfGraph);

        try {
            RulesEngine engine = ShaclRulesExec.create(engineType, baseGraph, ruleSet); //.setTrace(verbose);
            RuleSetEvaluation rsEval = engine.eval();
            assertNotNull(rsEval.baseGraph());
            assertNotNull(rsEval.inferredTriples());
            assertNotNull(rsEval.outputGraph());
            boolean pass = IsoMatcher.isomorphic(rsEval.inferredTriples(), expectedInfGraph);
            if ( !pass  ) {
                printFailedEvalTest(expectedInfGraph, rsEval);
                fail("Results do not match");
            }
            assertTrue(pass, "Inferred triples graph does not match the expected inference graph");

        } catch (RulesException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    static void printFailedEvalTest(Graph expected, RuleSetEvaluation rsEval) {
        Graph actual = rsEval.inferredTriples();
        PrintStream out = System.out;
        out.println("=======================================");
        out.println("---- Actual:");
        writeGraph(System.out, actual);
        out.println("---------------------------------------");
        out.println("---- Expected:");
        writeGraph(System.out, expected);
        out.println("---------------------------------------");
    }

    static void writeGraph(OutputStream out, Graph graph) {
        RDFWriter.source(graph).format(RDFFormat.TURTLE_FLAT).output(out);
    }
}
