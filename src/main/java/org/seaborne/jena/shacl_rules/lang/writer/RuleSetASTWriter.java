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

package org.seaborne.jena.shacl_rules.lang.writer;

import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapZero;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.sse.Tags;
import org.apache.jena.sparql.sse.writers.WriterExpr;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;

/**
 * Write a ruleset as an abstract syntax tree.
 */
public class RuleSetASTWriter {
    private final IndentedWriter out;
    private final PrefixMap prefixMap;
    private final IRIx base;
    private final NodeFormatter nodeFormatter;


    public static void write(RuleSet ruleSet) {
        //IndentedWriter w = IndentedWriter.stdout.clone().setEndOfLineMarker(" NL");
        IndentedWriter w = IndentedWriter.stdout.clone();
        try ( w ) {
            RuleSetASTWriter astw = new RuleSetASTWriter(w, ruleSet.getPrefixMap(), ruleSet.getBase());
            astw.writeRuleSet(ruleSet);
        } finally { w.flush(); }
    }

    private static final String tagRuleSet = "ruleset";
    private static final String tagData = "data";
    private static final String tagRule = "rule";

    private static final String tagHead = "head";
    private static final String tagBody = "body";

    private static final String tagFilter = "filter";
    private static final String tagBind = "filter";
    private static final String tagNot = "not";

    private static final String tagTriple = Tags.tagTriple;
    private static final String tagSubject = Tags.tagSubject;
    private static final String tagProperty = Tags.tagPredicate;
    private static final String tagObject = Tags.tagObject;

    private static final String tagBase = "base";
    private static final String tagPrefixes = "prefixes";

    // There is little value in using the visitor pattern due to detailed control of space between items.
    private RuleSetASTWriter(IndentedWriter output, PrefixMap prefixMap, IRIx baseIRI) {
        if  (prefixMap == null )
            prefixMap = PrefixMapZero.empty; // Prefixes.empty()

        this.out = Objects.requireNonNull(output);
        this.prefixMap = prefixMap;
        this.base = baseIRI;
        String baseStr = (baseIRI == null) ?null : baseIRI.str();
        this.nodeFormatter = new NodeFormatterTTL(baseStr, prefixMap);
    }

    public void writeRuleSet(RuleSet ruleSet) {
        Objects.requireNonNull(ruleSet);

        int depth = 0;

        // WriterBasePrefix but update for PrefixMap.

        if ( base != null ) {
            writeBase(base);
            depth++ ;
        }
        if ( prefixMap != null && !prefixMap.isEmpty() ) {
            writePrefixes(prefixMap);
            depth++ ;
        }

        out.print("(");
        out.print(tagRuleSet);
        out.println();
        out.incIndent();

        writeData(ruleSet);
        writeRules(ruleSet);
        out.decIndent();
        out.println(")");
        for ( int i = 0 ; i < depth ; i++ ) {
            out.decIndent();
            out.println(")");
        }
    }

    private void writeRules(RuleSet ruleSet) {
        List<Rule> rules = ruleSet.getRules();
        boolean blankLine = false;//ruleSet.hasData();
        for ( Rule rule : rules ) {
            if ( blankLine ) {
                out.println();
            }
            //blankLine = true;
            writeRule(rule);
        }
    }

    public void writeRule(Rule rule) {
        out.print("(");
        out.print(tagRule);
        out.printf(" [%s]", rule.id);
        out.println();
        out.incIndent();

        writeHead(rule);
        writeBody(rule);

        out.decIndent();
        out.println(")");
    }

    private void writeHead(Rule rule) {
        out.print("(");
        out.println(tagHead);
        out.incIndent();

        rule.getTripleTemplates().forEach(triple -> {
            writeTriple(triple);
            out.println();
        });

        out.decIndent();
        out.println(")");
    }

    private void writeBody(Rule rule) {
        out.print("(");
        out.println(tagBody);
        out.incIndent();

        writeRuleElements(rule.getBodyElements());
        out.ensureStartOfLine();

        out.decIndent();
        out.println(")");
    }

    private void writeRuleElements(List<RuleBodyElement> bodyElements) {
            // Without braces.
            boolean first = true;
            for ( RuleBodyElement elt : bodyElements ) {
                //if ( ! first ) {}
                first = false;

                switch (elt) {
                    case EltTriplePattern(Triple triplePattern) -> {
                        writeTriple(triplePattern);
                    }
                    case EltCondition(Expr condition) -> {
                        out.print("(");
                        out.print(tagFilter);
                        out.print(" ");
                        writeExpr(condition);
                        out.print(")");

                        // Multi-line
//                        out.println("(");
//                        out.println(tagFilter);
//                        out.incIndent();
//                        writeExpr(condition);
//                        out.println();
//                        out.decIndent();
//                        out.print(")");
                    }
                    case EltNegation(List<RuleBodyElement> inner) -> {
                        final int indentLevelNegation = 2 ;
                        out.print("(");
                        out.println(tagNot);
                        out.incIndent(indentLevelNegation);

                        writeRuleElements(inner);

                        out.decIndent(indentLevelNegation);
                        out.print(")");
                    }
                    case EltAssignment(Var var, Expr expression) -> {
                        out.print("(");
                        out.print(tagBind);
                        nodeFormatter.format(out, var);
                        out.print(" := ");
                        writeExpr(expression);
                        out.print(")");
                    }
                    case null -> {
                        throw new InternalErrorException();
                    }
                }
                out.println();
            }
        }

    private void printURI(String uriStr) {
        out.print("<");
        out.print(uriStr);
        out.print(">");
    }

    private void writeData(RuleSet ruleSet) {
        if ( ! ruleSet.hasData() )
            return;

        out.print("(");
        out.println(tagData);
        out.incIndent();

        List<Triple> data = ruleSet.getDataTriples();

        data.forEach(triple->{
            writeTriple(triple);
            out.println();
        });

        out.decIndent();
        out.println(")");
    }

    private void writeBase(IRIx base) {
        out.print("(");
        out.print(tagBase);
        out.print(" ");
        printURI(base.str());
        out.incIndent();
        out.println();
    }

    private void writePrefixes(PrefixMap prefixMap) {
        out.print("(");
        out.print(tagPrefixes);
        // Indent 2 levels
        out.incIndent();
        out.print(" (");
        out.incIndent();
        prefixMap.forEach((prefix, uriStr) -> {
            // Base relative URI = but not prefix mappings!
            out.println();
            out.print("(");
            out.print(prefix);
            out.print(": ");
            printURI(uriStr);
            out.print(")");
        });
        out.println(")");
        // Only one back
        out.decIndent();
        out.ensureStartOfLine();
    }

    // Space then triple.
    private void writeTriple(Triple triple) {
        nodeFormatter.format(out, triple.getSubject());
        out.print(" ");
        nodeFormatter.format(out, triple.getPredicate());
        out.print(" ");
        nodeFormatter.format(out, triple.getObject());
        out.print(" .");
    }

    private void writeExpr(Expr expr) {
        SerializationContext sCxt = new SerializationContext(Prefixes.adapt(prefixMap));
        WriterExpr.output(out, expr, sCxt);
    }
}