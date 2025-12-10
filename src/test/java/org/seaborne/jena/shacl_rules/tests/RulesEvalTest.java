/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.jena.shacl_rules.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.graph.GraphZero;
import org.apache.jena.sparql.util.IsoMatcher;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.exec.RulesEngineFwdSimple;
import org.seaborne.jena.shacl_rules.junit.VocabRulesTests;
import org.seaborne.jena.shacl_rules.lang.ShaclRulesParseException;

public class RulesEvalTest implements Runnable {

    private final ManifestEntry testItem;
    private final boolean positiveTest;

    public RulesEvalTest(ManifestEntry entry, String base, boolean positiveTest) {
        this.testItem = entry;
        this.positiveTest = positiveTest;
    }

    @Override
    public void run() {
        Graph graph = testItem.getGraph();
        String name = testItem.getName();
        Node action = testItem.getAction();

        Node nRuleSet = G.getOneSP(graph, action, VocabRulesTests.ruleSet);
        String testFilename = FileOps.basename(nRuleSet.getURI());
        RuleSet ruleSet;
        try {
            ruleSet = ShaclRulesParser.parseFile(nRuleSet.getURI());
        } catch ( ShaclRulesParseException parseEx) {
            System.out.println("** Parse error ("+testFilename+")");
            ruleSet = null;
            fail("Parse error in rule set ("+testFilename+")");
            return;
        }

        Node nData = G.getOneSP(graph, action, VocabRulesTests.data);

        Graph input = ( nData == null ) ? GraphZero.instance() : read(nData);

        boolean verbose = false;
        RuleSetEvaluation e = RulesEngineFwdSimple.build(input, ruleSet).setTrace(false).eval();

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
        out.println("----Expected:");
        write(System.out, expected);
        out.println("---------------------------------------");
    }

    private static void write(OutputStream out, Graph graph) {
        RDFWriter.source(graph).format(RDFFormat.TURTLE_LONG).output(out);
    }

    private static Graph read(Node g) {
        return RDFParser.source(g.getURI()).toGraph();
    }


}
