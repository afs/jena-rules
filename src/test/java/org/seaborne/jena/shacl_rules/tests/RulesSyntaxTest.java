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
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.NotFoundException;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;

public class RulesSyntaxTest implements Runnable {

    final private boolean expectLegalSyntax;
    final private ManifestEntry testEntry;
    final private String testBase;
    //final private Lang lang;
    final private String filename;

    public RulesSyntaxTest(ManifestEntry entry, String base, boolean positiveTest) {
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

        try {
            parseForTest(filename, base, allowWarnings, expectLegalSyntax);
            if (! expectLegalSyntax ) {
                printFile(filename);
                fail("Parsing succeeded in a bad syntax test");
            }
        } catch(ShaclRulesParseException | RiotException ex) {
            if ( expectLegalSyntax ) {
                printFile(filename);
                //ex.printStackTrace();
                fail("Parse error: "+ex.getMessage());
            }
        }
    }

    private static void printFile(String filename) {
        String fn = IRILib.IRIToFilename(filename);
        String s = IO.readWholeFileAsUTF8(fn);
        System.err.println("== "+filename);
        System.err.print(s);
    }

    private static void parseForTest(String filename, String base, boolean allowWarnings, boolean expectLegalSyntax) {
        if ( expectLegalSyntax ) {
            ShaclRulesParser.parseFile(filename);
            return;
        }

        String level = LogCtl.getLevel(ShaclRulesParser.parserLogger);
        LogCtl.setLevel(ShaclRulesParser.parserLogger, "FATAL");
        try {
            // Expect errors - so don't log them.
            ShaclRulesParser.parseFile(filename);
        } finally {
            LogCtl.setLevel(ShaclRulesParser.parserLogger, level);
        }
    }
}
