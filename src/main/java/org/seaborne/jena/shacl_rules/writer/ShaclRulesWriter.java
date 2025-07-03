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

package org.seaborne.jena.shacl_rules.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL_MultiLine;
import org.apache.jena.riot.system.*;
import org.apache.jena.riot.writer.DirectiveStyle;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleHead;
import org.seaborne.jena.shacl_rules.RuleSet;

public class ShaclRulesWriter {

    enum Style { Flat, MultiLine }

    public static void printBasic(RuleSet ruleSet) {
        ruleSet.getRules().forEach(r -> {
            System.out.println(r);
        });
    }

    public static void print(RuleSet ruleSet) {
        print(System.out, ruleSet, true);
    }

    public static void print(RuleSet ruleSet, boolean flatMode) {
        print(System.out, ruleSet, flatMode);
    }

    public static void print(OutputStream outStream, RuleSet ruleSet, boolean flatMode) {

        Style style = flatMode ? Style.Flat : Style.MultiLine;

        IndentedWriter output = new IndentedWriter(outStream);
        try {
            internalPrint(output, ruleSet, style);
        } finally {
            output.flush();
        }
    }

    /** Write a rule */
    public static void print(Rule rule) {
        print(System.out, rule, null, true);
    }

    /** Write a rule using a prefix map (not printed). */
    public static void print(Rule rule, PrefixMap prefixMap) {
        print(System.out, rule, prefixMap, true);
    }

    /** Write a rule (no prologue). */
    public static void print(OutputStream outStream, Rule rule, PrefixMap prefixMap, boolean flatMode) {
        IndentedWriter output = new IndentedWriter(outStream);
        try {
            print(output, rule, prefixMap, flatMode);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void print(IndentedWriter output, Rule rule, PrefixMap prefixMap, boolean flatMode) {
        Style style = flatMode ? Style.Flat : Style.MultiLine;
        try {
            internalPrint(output, rule, style, prefixMap);
        } finally {
            output.flush();
        }
    }

    public static void print(IndentedWriter output, Rule rule, boolean flatMode) {
        Style style = flatMode ? Style.Flat : Style.MultiLine;
        try {
            internalPrint(output, rule, style, null);
        } finally {
            output.flush();
        }
    }

    // ---------------------

    private static void internalPrint(IndentedWriter out, RuleSet ruleSet, Style style) {
        IRIx baseIRI = null;
        PrefixMap prefixMap = null;
        if ( ruleSet != null ) {
            baseIRI = ruleSet.getBase();
            prefixMap = ruleSet.getPrefixMap();
        }
        if ( prefixMap == null )
            // Helps switching to/from PrefixMappings.
            prefixMap = PrefixMapFactory.create();

        RuleSetWriter srw = new RuleSetWriter(out, prefixMap, baseIRI, style);
        srw.writeRuleSet(ruleSet);
    }

    private static void internalPrint(IndentedWriter out, Rule rule, Style style, PrefixMap prefixMap) {
//        IRIx baseIRI = ruleSet.getBase();
//        PrefixMap prefixMap = ruleSet.getPrefixMap();
//        if ( prefixMap == null )
//            prefixMap = PrefixMapFactory.create();
        //PrefixMap prefixMap = PrefixMapFactory.create();

        RuleSetWriter srw = new RuleSetWriter(out, prefixMap, null, style);
        srw.writeRule(rule);
    }



    static class RuleSetWriter {

        private final IndentedWriter out;
        private final PrefixMap prefixMap;
        private final IRIx base;
        private final NodeFormatter nodeFormatter;
        private final SerializationContext sCxt;

        private final Style style;

        // There is little value in using the visitor pattern due to detailed control of space between items.
        private RuleSetWriter(IndentedWriter output, PrefixMap prefixMap, IRIx baseIRI, Style style) {
            if  (prefixMap == null )
                prefixMap = PrefixMapZero.empty; // Prefixes.empty()

            this.out = Objects.requireNonNull(output);
            this.prefixMap = prefixMap;
            this.base = baseIRI;
            String baseStr = (baseIRI == null) ?null : baseIRI.str();

            this.nodeFormatter = new NodeFormatterTTL_MultiLine(baseStr, prefixMap);
            // XXX Replace me.
            this.sCxt = new SerializationContext(Prefixes.adapt(prefixMap));
            this.style = Objects.requireNonNull(style);
        }

        private void writeRuleSet(RuleSet ruleSet) {
            Objects.requireNonNull(ruleSet);
            if ( base != null )
                RiotLib.writeBase(out, base.str(), DirectiveStyle.KEYWORD);
            if ( prefixMap != null )
                RiotLib.writePrefixes(out, prefixMap, DirectiveStyle.KEYWORD);
            if ( ( base != null || !prefixMap.isEmpty() ) && !ruleSet.isEmpty() )
                out.println();

            writeData(ruleSet);

            List<Rule> rules = ruleSet.getRules();
            boolean first = true;

            for ( Rule rule : rules ) {
                if ( ! first ) {
                    if ( style == Style.MultiLine )
                        out.println();
                }

                first = false;

                writeRule(rule);
            }
        }

        private void writeData(RuleSet ruleSet) {
            List<Triple> data = ruleSet.getDataTriples();
            if ( data == null || data.isEmpty() )
                return;

            out.print("DATA {");
            if ( style == Style.Flat || data.size() == 1 ) {
                data.forEach(triple->{
                    out.print(" ");
                    writeTriple(triple);
                });
                out.println(" }");
                return;
            }
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

        private void writeRule(Rule rule) {
            out.print("RULE ");
            writeHead(rule);
            if ( style == Style.MultiLine )
                out.println();
            else
                out.print(" ");
            out.print("WHERE ");
            writeBody(rule);
            out.println();
        }

        private void writeHead(Rule rule) {
            RuleHead head = rule.getHead();
            out.print("{");
            head.forEach(triple -> {
                out.print(" ");
                writeTriple(triple);
            });
            out.print(" }");
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

        private void writeBody(Rule rule) {
            // The element block in indented. Later ...
            int indent = 0 ;

            switch(style) {
                case Flat -> {
                    out.setFlatMode(true);
                    out.print("{");
                }
                case MultiLine -> {
                    out.print("{");
                    out.println();
                    //out.incIndent(indent);
                }
            }
            // Without braces.
            IndentedLineBuffer outx = new IndentedLineBuffer();
            FormatterElement.format(outx, sCxt, rule.getBody().asElement());
            String x = outx.asString();
            // Remove outer {}s. Put back leading space.
            x = " "+x.substring(1, x.length()-1);
            if ( style == Style.Flat ) {
                //x = x.replace("\n", " ");
                x = x.replaceAll("  +", " ");
            }

            out.print(x);

            switch(style) {
                case Flat -> {
                    out.print(" }");
                    out.setFlatMode(false);
                }
                case MultiLine ->{
                    out.decIndent(indent);
                    out.println("}");
                }
            }
            out.flush();
        }
    }
}
