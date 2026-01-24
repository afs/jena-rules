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

import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.shared.NotFoundException;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.sys.WellFormed;

public class RulesWellFormednessTest implements Runnable {

    final private boolean expectLegalSyntax;
    final private ManifestEntry testEntry;
    final private String testBase;
    //final private Lang lang;
    final private String filename;

    public RulesWellFormednessTest(ManifestEntry entry, String base, boolean positiveTest) {
        this.testEntry = entry;
        this.testBase = base;
        this.expectLegalSyntax = positiveTest;
        this.filename = entry.getAction().getURI();
        // RDF vs SHACL-R
        //this.lang = lang;
    }

    @Override
    public void run() {
        // Check so the parse step does not confuse missing with bad syntax.
        String fn = IRILib.IRIToFilename(filename);
        if ( ! FileOps.exists(fn) ) {
            throw new NotFoundException("File not found: "+filename) {
                @Override public Throwable fillInStackTrace() { return this; }
            };
        }
        String base = testBase;
        if ( base == null )
            base = filename;

        boolean allowWarnings = false;

        // Must pass as valid syntax
        RuleSet ruleSet = parseForTest(filename, base, allowWarnings);

        try {

            WellFormed.checkWellFormed(ruleSet);
            if (! expectLegalSyntax ) {
                printFile(filename);
                fail("WellFormed check succeeded in a negative test");
            }
        } catch(WellFormed.NotWellFormedException ex) {
            if ( expectLegalSyntax ) {
                printFile(filename);
                //ex.printStackTrace();
                fail("WellFormed check failed: "+ex.getMessage());
            }
        }
    }

    private static void printFile(String filename) {
        String fn = IRILib.IRIToFilename(filename);
        String s = IO.readWholeFileAsUTF8(fn);
        System.err.println();
        System.err.println("== "+filename);
        System.err.print(s);
    }

    private static RuleSet parseForTest(String filename, String base, boolean allowWarnings) {
        RuleSet ruleSet = ShaclRulesParser.parseFile(filename);
        return ruleSet;
    }
}
