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

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.atlas.RuntimeIOException;
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
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.RulesEngine;
import org.seaborne.jena.srl.ShaclRulesExec;
import org.seaborne.jena.srl.ShaclRulesParser;
import org.seaborne.jena.srl.examine.Examine;
import org.seaborne.jena.srl.exec.EngineType;
import org.seaborne.jena.srl.exec.RuleSetEvaluation;
import org.seaborne.jena.srl.junit.VocabRulesTests;
import org.seaborne.jena.srl.lang.parser.ShaclRulesParseException;

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


        String FN = IRILib.IRIToFilename(nRuleSet.getURI());
        if ( ! FileOps.exists(FN) ) {
            System.err.println("No such file: "+FN);
            throw new RuntimeException("No such file: "+FN);
        }
        try {
            ruleSet = ShaclRulesParser.parseFile(nRuleSet.getURI());
        } catch ( RuntimeIOException ex) {
            if ( ex.getCause() instanceof FileNotFoundException ) {
                System.out.println("** RuleSet not found: "+nRuleSet.getURI());
                fail("RuleSet not found ("+testFilename+")");
            }
            ruleSet = null;
            throw ex;
        } catch ( ShaclRulesParseException parseEx) {
            System.out.println("** Parse error ("+testFilename+")");
            ruleSet = null;
            fail("Parse error in rule set ("+testFilename+")");
            return;
        }

        Node nData = G.getOneSP(itemGraph, action, VocabRulesTests.data);
        Graph input = ( nData == null ) ? GraphZero.instance() : read(nData);

        Examine.EXAMINE = false;
        RulesEngine rulesEngine = ShaclRulesExec.create(engineType, input, ruleSet);

        RuleSetEvaluation e = rulesEngine.eval();

        Graph outcome = e.inferredTriples();
        Node nResult = testItem.getResult();
        Graph resultsExpected = read(nResult);

        boolean pass = IsoMatcher.isomorphic(resultsExpected, outcome);
        if (! pass ) {
            printFailedEvalTest(testItem, resultsExpected, outcome);
            fail("Results do not match: " + testItem.getName());
        }
    }

    private void printFailedEvalTest(ManifestEntry entry, Graph expected, Graph actual) {
        PrintStream out = System.out;
        out.println("=======================================");
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

    private static Graph read(Node g) {
        String URI = g.getURI();
        String FN = IRILib.IRIToFilename(URI);
        if ( ! FileOps.exists(FN) ) {
            System.err.println("No such file: "+FN);
            throw new RuntimeException("No such file: "+FN);
        }
        return RDFParser.source(URI).toGraph();
    }
}
