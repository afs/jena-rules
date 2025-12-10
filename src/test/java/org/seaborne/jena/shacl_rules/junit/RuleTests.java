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

package org.seaborne.jena.shacl_rules.junit;

import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.SplitIRI;
import org.seaborne.jena.shacl_rules.tests.RulesEvalTest;
import org.seaborne.jena.shacl_rules.tests.RulesSyntaxTest;

public class RuleTests {

    /** Create a Rules test - or return null for "unrecognized" */
    public static Runnable makeRuleTest(ManifestEntry entry) {
        //Resource manifest = entry.getManifest();
        Node item = entry.getEntry();
        String testName = entry.getName();
        Node action = entry.getAction();
        Node result = entry.getResult();

        //String labelPrefix = "[Rules]";
        String labelPrefix = null;

        try {
            Node testType = entry.getTestType();
            if ( testType == null )
                throw new JenaException("Can't determine the test type");

            if ( labelPrefix != null )
                testName = labelPrefix+testName;

            Node input = action;
            Node output = result;

            // Tests may assume a certain base URI.
            String assumedBase = entry.getManifest().getTestBase();
            String testURI = rebase(input, assumedBase);

            // == Syntax
            if ( testType.equals(VocabRulesTests.TestPositiveSyntaxRules) )
                return new RulesSyntaxTest(entry, testURI, true);
            if ( testType.equals(VocabRulesTests.TestNegativeSyntaxRules) )
                return new RulesSyntaxTest(entry, testURI, false);

            // == Eval
            if ( testType.equals(VocabRulesTests.TestPositiveEvalRules) )
                return new RulesEvalTest(entry, testURI, true);
            if ( testType.equals(VocabRulesTests.TestNegativeEvalRules) )
                return new RulesEvalTest(entry, testURI, false);

            Log.warn(RuleTests.class, "Test not classified - "+entry.getName()+" <"+entry.getURI()+"> Type:"+NodeFmtLib.displayStr(testType));

            return null;
            //return new SurpressedTest(entry);

        } catch (Exception ex)
        {
            ex.printStackTrace(System.err);
            System.err.println("Failed to grok test : " + testName);
            return null;
        }
    }

    private static String rebase(Node input, String baseIRI) {
        if ( input.isBlank() )
            return baseIRI;
        String inputURI = input.getURI();
        if ( baseIRI == null )
            return inputURI;
        int splitPoint = SplitIRI.splitpoint(input.getURI());
        if ( splitPoint < 0 )
            return inputURI;

        String x = SplitIRI.localname(inputURI) ;
        baseIRI = baseIRI+x;
        return baseIRI;
    }
}
