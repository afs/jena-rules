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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.jena.arq.junit.manifest.AbstractManifestTest;
import org.apache.jena.arq.junit.manifest.ManifestEntry;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.shared.NotFoundException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.QueryCompare;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.syntaxtransform.ElementTransform;
import org.apache.jena.sparql.syntax.syntaxtransform.ElementTransformCopyBase;
import org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps;
import org.apache.jena.sparql.util.NodeIsomorphismMap;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.SRL;
import org.seaborne.jena.srl.exec.RulesLibSparql;
import org.seaborne.jena.srl.lang.RulesSyntax;
import org.seaborne.jena.srl.lang.parser.SRLParseException;

public class RulesTranslationTest extends AbstractManifestTest {

    final private boolean expectLegalSyntax;
    final private String testBase;
    //final private Lang lang;
    final private String fnSRL;
    final private String fnConstruct;
    private RulesSyntax rulesSyntax;

    public RulesTranslationTest(ManifestEntry entry, String base, RulesSyntax syntax, boolean positiveTest) {
        super(entry);
        this.testBase = base;
        this.expectLegalSyntax = positiveTest;
        this.fnSRL = entry.getAction().getURI();
        this.fnConstruct = entry.getResult().getURI();
        this.rulesSyntax = syntax;
    }


    static class TransformPathBlockToTripleBlock extends ElementTransformCopyBase {
        @Override
        public Element transform(ElementPathBlock elt) {
            BasicPattern bgp = new BasicPattern();

            elt.getPattern().getList().forEach(tp->{
                Triple t = tp.asTriple();
                if ( t == null )
                    throw new QueryException("Comlex path found: "+elt.getPattern());
                bgp.add(t);
            });
            return new ElementTriplesBlock(bgp);
        }
    }


    @Override
    public void runTest() {
        checkFileExists(fnSRL);
        checkFileExists(fnConstruct);

        ElementTransform pathToTriples = new TransformPathBlockToTripleBlock();
        String srlRuleStr = readFile(fnSRL);
        String sparqlQueryStr = readFile(fnConstruct);
        String base = testBase;
        boolean allowWarnings = false;
        try {
            try {
                RuleSet rs = SRL.parseString(srlRuleStr);
                assertEquals(1, rs.getRules().size(), ()->"Bad test: expected exactly one rule in the test file");
                Rule rule = rs.getRules().getFirst();

                Query expectedQuery = QueryFactory.create(sparqlQueryStr);
                assertEquals(QueryType.CONSTRUCT, expectedQuery.queryType(), ()->"Expected a CONSTRUCT query");

                Query actualQuery = translateRuleToConstruct(rule);
                assertNotNull(actualQuery);
                assertEquals(QueryType.CONSTRUCT, actualQuery.queryType(), ()->"Translation is not a CONSTRUCT query");

                // Adjust prefixes.
                actualQuery.getPrefixMapping().setNsPrefix("", "http://example/");

//                expectedQuery.getPrefixMapping().setNsPrefixes(actualQuery.getPrefixMapping());
//                actualQuery.getPrefixMapping().setNsPrefixes(expectedQuery.getPrefixMapping());

                // TriplesBlock - used by RulesSparqlLib - actualQuery
                // PathBlock - used by parser - expectedQuery => convert to TriplesBlock form (and proper paths are illegal).

                // ---- 1
                // Turn a parsed query into the form the translation has (PathBlock to TripleBlock)
                expectedQuery = QueryTransformOps.transform(expectedQuery, pathToTriples);
                boolean b = QueryCompare.equals(actualQuery, expectedQuery);
                assertTrue(b);

                // ---- 2
//                // rebuild via string
//                actualQuery =  QueryFactory.create(actualQuery.toString());
//                boolean b = QueryCompare.equals(actualQuery, expectedQuery);
                if ( ! b ) {
                    System.out.println("DIFFERENT");
                    System.out.println("Expected:");
                    System.out.println(expectedQuery);
                    System.out.println("Actual:");
                    System.out.println(actualQuery);
                    fail("Queries different");
                }

                // ---- 3
                // By algebra
                assertEquals(Algebra.compile(expectedQuery), Algebra.compile(actualQuery));
                // ... then construct template
                boolean b1 = expectedQuery.getConstructTemplate().equalIso(actualQuery.getConstructTemplate(), new NodeIsomorphismMap());
                assertTrue(b1);

                // 4
                // By string.
                assertEquals(expectedQuery.toString(), actualQuery.toString());

            } catch(SRLParseException  ex) {
                if ( expectLegalSyntax ) {
                    System.out.print(srlRuleStr);
                    if ( !srlRuleStr.endsWith("\n"))
                        System.out.println();
                    fail("SRL parse error: "+ex.getMessage());
                }
            } catch(QueryParseException ex) {
                if ( expectLegalSyntax ) {
                    System.out.print(sparqlQueryStr);
                    if ( !sparqlQueryStr.endsWith("\n"))
                        System.out.println();
                    fail("SPARQL parse error: "+ex.getMessage());
                }
            } catch(RuntimeException ex) {
                ex.printStackTrace();
                throw ex;
            }
        } catch (Throwable th) {
//            System.out.println(th.getClass().getSimpleName());
//            System.out.println(th.getMessage());
//            //th.printStackTrace();
            throw th;
        }
    }

    private Query translateRuleToConstruct(Rule rule) {
        return RulesLibSparql.ruleToConstruct(rule);
    }

    private static void checkFileExists(String filename) {
        String fn = IRILib.IRIToFilename(filename);
        if ( ! FileOps.exists(fn) ) {
            throw new NotFoundException("File not found: "+filename) {
                @Override public Throwable fillInStackTrace() { return this; }
            };
        }
    }

    private static String readFile(String filename) {
        String fn = IRILib.IRIToFilename(filename);
        String s = IO.readWholeFileAsUTF8(fn);
        return s;
    }
}
