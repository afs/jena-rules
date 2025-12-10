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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.seaborne.jena.shacl_rules.lang.writer;

import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL_MultiLine;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapZero;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.writer.DirectiveStyle;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.sse.writers.WriterExpr;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.lang.RuleElement;

/**
 * Write a ruleset as an abstract syntax tree.
 */
public class RuleSetASTWriter {
    private final IndentedWriter out;
    private final PrefixMap prefixMap;
    private final IRIx base;
    private final NodeFormatter nodeFormatter;


    public static void dump(RuleSet ruleSet) {
        IndentedWriter w = IndentedWriter.stdout.clone();
        RuleSetASTWriter astw = new RuleSetASTWriter(w, ruleSet.getPrefixMap(), IRIx.create("http://base/"));
        astw.writeRuleSet(ruleSet);
        w.flush();
    }

    // There is little value in using the visitor pattern due to detailed control of space between items.
    private RuleSetASTWriter(IndentedWriter output, PrefixMap prefixMap, IRIx baseIRI) {
        if  (prefixMap == null )
            prefixMap = PrefixMapZero.empty; // Prefixes.empty()

        this.out = Objects.requireNonNull(output);
        this.prefixMap = prefixMap;
        this.base = baseIRI;
        String baseStr = (baseIRI == null) ?null : baseIRI.str();

        this.nodeFormatter = new NodeFormatterTTL_MultiLine(baseStr, prefixMap);
    }

    public void writeRuleSet(RuleSet ruleSet) {
        Objects.requireNonNull(ruleSet);
        if ( base != null )
            RiotLib.writeBase(out, base.str(), DirectiveStyle.KEYWORD);
        if ( prefixMap != null )
            RiotLib.writePrefixes(out, prefixMap, DirectiveStyle.KEYWORD);
        if ( ( base != null || !prefixMap.isEmpty() ) && !ruleSet.isEmpty() )
            out.println();

        writeData(ruleSet);

        List<Rule> rules = ruleSet.getRules();
        boolean blankLine = ruleSet.hasData();

        for ( Rule rule : rules ) {
            if ( blankLine ) {
                out.println();
            }
            blankLine = true;
            writeRule(rule);
        }
    }

    private void writeData(RuleSet ruleSet) {
        if ( ! ruleSet.hasData() )
            return;
        List<Triple> data = ruleSet.getDataTriples();

        out.println();
        out.incIndent();
        data.forEach(triple->{
            writeTriple(triple);
            out.println();
        });
        out.decIndent();
        out.println("}");
        out.println();
    }

    public void writeRule(Rule rule) {
        out.print("RULE ");
        writeHead(rule);
        writeBody(rule);
    }

    private void writeHead(Rule rule) {
        rule.getTripleTemplates().forEach(triple -> {
            out.print(" ");
            writeTriple(triple);
        });
    }

    private void writeBody(Rule rule) {
        writeRuleElements(rule.getBodyElements());
        out.ensureStartOfLine();
        out.flush();
    }

    private void writeRuleElements(List<RuleElement> bodyElements) {
        // Without braces.
        boolean first = true;
        for ( RuleElement elt : bodyElements ) {
            //if ( ! first ) {}
            first = false;

            switch (elt) {
                case RuleElement.EltTriplePattern(Triple triplePattern) -> {
                    writeTriple(triplePattern);
                }
                case RuleElement.EltCondition(Expr condition) -> {
                    out.write("FILTER");
                    writeExpr(condition);
                }
                case RuleElement.EltNegation(List<RuleElement> inner) -> {
                    out.write("NOT {");
                    out.println();
                    final int indentLevelNegation = 4 ;
                    out.incIndent(indentLevelNegation);
                    writeRuleElements(inner);
                    out.decIndent(indentLevelNegation);
                    out.println();
                    out.write(" }");
                }
                case RuleElement.EltAssignment(Var var, Expr expression) -> {
                    out.write("BIND( ");
                    writeExpr(expression);
                    out.write(" AS ");
                    nodeFormatter.format(out, var);
                    out.write(")");
                }
                case null -> {
                    throw new InternalErrorException();
                }
            }
        }
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