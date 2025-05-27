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

package org.seaborne.jena.shacl_rules.runner;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.arq.junit.LibTestSetup;
import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.jena.shacl_rules.tests.RulesEvalTest;
import org.seaborne.jena.shacl_rules.tests.RulesSyntaxTest;

public class RuleTests {

    /** Create a Rules test - or return null for "unrecognized" */
    public static Runnable makeRuleTest(ManifestEntry entry) {
        //Resource manifest = entry.getManifest();
        Resource item = entry.getEntry();
        String testName = entry.getName();
        Resource action = entry.getAction();
        Resource result = entry.getResult();

        String labelPrefix = "[Rules]";

        try {
            Resource testType = LibTestSetup.getResource(item, RDF.type);
            if ( testType == null )
                throw new JenaException("Can't determine the test type");

            if ( labelPrefix != null )
                testName = labelPrefix+testName;

            Resource input = action;
            Resource output = result;

            // Some tests assume a certain base URI.

            // == Syntax tests.

            String assumedBase = entry.getManifest().getTestBase();
            String base = rebase(input, assumedBase);

            // == Syntax
            if ( testType.equals(VocabRulesTests.TestPositiveSyntaxRules) )
                return new RulesSyntaxTest(entry, base, true);
            if ( testType.equals(VocabRulesTests.TestNegativeSyntaxRules) )
                return new RulesSyntaxTest(entry, base, false);

            // == Eval
            if ( testType.equals(VocabRulesTests.TestEvalRules) )
                return new RulesEvalTest(entry, base, true);
            if ( testType.equals(VocabRulesTests.TestNegativeEvalRules) )
                return new RulesEvalTest(entry, base, false);

            return null;
            //return new SurpressedTest(entry);

        } catch (Exception ex)
        {
            ex.printStackTrace(System.err);
            System.err.println("Failed to grok test : " + testName);
            return null;
        }
    }

    private static String rebase(Resource input, String baseIRI) {
        String x = input.getLocalName();
        // Yuk, yuk, yuk.
        baseIRI = baseIRI+x;
        return baseIRI;
    }

    static Set<String> allowWarningSet = new HashSet<>();
    static {
        // example:
        //allowWarningSet.add("#turtle-eval-bad-01");
    }
}
