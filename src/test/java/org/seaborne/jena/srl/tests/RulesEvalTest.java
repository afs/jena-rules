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

package org.seaborne.jena.srl.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.graph.GraphZero;
import org.apache.jena.sparql.util.IsoMatcher;
import org.apache.jena.system.G;
import org.seaborne.jena.srl.*;
import org.seaborne.jena.srl.examine.Examine;
import org.seaborne.jena.srl.exec.EngineType;
import org.seaborne.jena.srl.exec.RuleSetEvaluation;
import org.seaborne.jena.srl.junit.VocabRulesTests;
import org.seaborne.jena.srl.lang.parser.SRLParseException;

public class RulesEvalTest implements Runnable {

    private final ManifestEntry testItem;
    private final EngineType engineType;
    private final boolean positiveTest;

    public RulesEvalTest(ManifestEntry entry, String base, EngineType engineType, boolean positiveTest) {
        this.testItem = entry;
        this.engineType = engineType;
        this.positiveTest = positiveTest;
    }

    @Override
    public void run() {
        run(engineType);
    }

    private void run(EngineType engineType) {
        //System.out.println(engineType);
        Graph itemGraph = testItem.getGraph();
        String itemName = testItem.getName();
        Node action = testItem.getAction();

        Node nRuleSet = G.getOneSP(itemGraph, action, VocabRulesTests.ruleSet);
        String testFilename = FileOps.basename(nRuleSet.getURI());
        RuleSet ruleSet;

        String URI = checkForFile(nRuleSet);
        try {
            ruleSet = ShaclRulesParser.parseFile(URI);
        } catch ( SRLParseException parseEx) {
            System.out.println("** Parse error ("+testFilename+")");
            ruleSet = null;
            fail("Parse error in rule set ("+testFilename+")");
            return;
        }

        Node nData = G.getOneSP(itemGraph, action, VocabRulesTests.data);
        Graph input = ( nData == null ) ? GraphZero.instance() : read(nData);

        Examine.EXAMINE = false;
        RulesEngine rulesEngine = ShaclRulesExec.create(engineType, input, ruleSet);

        RuleSetEvaluation evaluation = rulesEngine.eval();

        Graph outcome = evaluation.inferredTriples();
        Node nResult = testItem.getResult();
        Graph resultsExpected = read(nResult);

        boolean pass = IsoMatcher.isomorphic(resultsExpected, outcome);
        if (! pass ) {
            printFailedEvalTest(testItem, ruleSet, evaluation, resultsExpected, outcome);
            fail("Results do not match: " + testItem.getName());
        }
    }

    private void printFailedEvalTest(ManifestEntry entry, RuleSet ruleSet, RuleSetEvaluation e, Graph expected, Graph actual) {
        PrintStream out = System.out;

        out.println("=======================================");
        ShaclRulesWriter.print(ruleSet);

        if ( true ) {
            out.println("---------------------------------------");
            System.out.println("-- Base graph");
            write(System.out, e.baseGraph());
            System.out.println("-- Inferred graph");
            write(System.out, e.inferredTriples());
            System.out.println("-- Output graph");
            write(System.out, e.outputGraph());
        }
        out.println("---------------------------------------");
        out.println("---- Failure: " + entry.getName());
        out.println("---- Actual:");
        write(System.out, actual);
        out.println("---------------------------------------");
        out.println("---- Expected:");
        write(System.out, expected);
        out.println("---------------------------------------");
    }

    private static void write(OutputStream out, Graph graph) {
        RDFWriter.source(graph).format(RDFFormat.TURTLE_FLAT).output(out);
    }

    private static String checkForFile(Node file) {
        String URI = file.getURI();
        String FN = IRILib.IRIToFilename(URI);
        if ( ! FileOps.exists(FN) ) {
            System.err.println("No such file: "+FN);
            throw new RuntimeException("No such file: "+FN);
        }
        return URI;
    }

    private static Graph read(Node g) {
        String URI = checkForFile(g);
        return RDFParser.source(URI).toGraph();
    }
}
