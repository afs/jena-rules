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

package org.seaborne.jena.shacl_rules.cmds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.cmd.CmdException;
import org.apache.jena.cmd.TerminationException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMemFactory;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.*;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.seaborne.jena.shacl_rules.*;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.exec.RulesEngineRegistry;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;
import org.seaborne.jena.shacl_rules.sys.RecursionChecker.RecursionException;
import org.seaborne.jena.shacl_rules.sys.Stratification.StratificationException;
import org.seaborne.jena.shacl_rules.sys.SysJenaRules;
import org.seaborne.jena.shacl_rules.sys.WellFormed.NotWellFormedException;

public class rules_eval extends CmdRules {

    static { JenaSystem.init(); }

    public static void main(String...argv) {
        new rules_eval(argv).mainRun();
    }

    protected rules_eval(String[] argv) {
        super(argv);
    }

    @Override
    protected void exec() {
        //rules ruleFile [data]

        String rulesFile = null;
        String dataFile = null;

        switch(positionals.size()) {
            case 1 -> {
                rulesFile = positionals.get(0);
                if ( ! FileOps.exists(rulesFile) )
                    throw new CmdException("No such file: "+rulesFile);
            }
            case 2 -> {
                rulesFile = positionals.get(0);
                dataFile = positionals.get(1);
                if ( ! FileOps.exists(rulesFile) )
                    throw new CmdException("No such file: "+rulesFile);
                if ( ! FileOps.exists(dataFile) )
                    throw new CmdException("No such file: "+dataFile);
            }
            default ->
                throw new CmdException("Usage: rules exec RulesFile [DataFile]");
        }

        RuleSet ruleSet;
        try {
            ruleSet = ShaclRulesParser.parseFile(rulesFile);
        } catch (ShaclRulesParseException ex) {
            System.err.println("Syntax error");
            throw messageAndTerminate(ex, 1);
        }
        Graph data = GraphMemFactory.createDefaultGraph();

        if ( dataFile != null ) {
            RDFParser.source(dataFile).parse(data);
        } else {
            if ( ruleSet.hasPrefixMap() )
                data.getPrefixMapping().setNsPrefixes(Prefixes.adapt(ruleSet.getPrefixMap()));
        }

        boolean verbose = super.isVerbose();
        try {
            RulesEngine engine = defaultRulesEngine(data, ruleSet).setTrace(verbose);
            exec(ruleSet, data, engine);
        }
        catch (NotWellFormedException ex) {
            System.err.println("Not wellformed");
            throw messageAndTerminate(ex, 1);
        }
        catch (StratificationException ex) {
            System.err.println("Stratification failure");
            throw messageAndTerminate(ex, 1);
        }
        catch (RecursionException ex) {
            System.err.println("Recursion error");
            throw messageAndTerminate(ex, 1);
        }
    }

    private static RuntimeException messageAndTerminate(RuntimeException ex, int rc) {
        System.err.print("  ");
        System.err.print(ex.getMessage());
        System.err.println();
        throw new TerminationException(rc);
    }

    private static RulesEngine defaultRulesEngine(Graph data, RuleSet ruleSet) {
        RulesEngine engine = RulesEngineRegistry.get()
                .create(SysJenaRules.dftEngineType, data, null, ruleSet, Rules.getContext());
        return engine;
    }

    private static void exec(RuleSet ruleSet, Graph data, RulesEngine engine) {
        RuleSetEvaluation e = engine.eval();
        Graph accGraph = e.inferredTriples();
        Graph output = e.outputGraph();
//        if ( e.rounds() >= 0 ) {
//            System.out.println("## Rounds: "+e.rounds());
//            System.out.println();
//        }

        boolean printRuleSet = true;
        boolean printBaseGraph = true;
        boolean printRulesData = ! printRuleSet;
        boolean printInfGraph = true;
        boolean printOutputGraph = true;

        boolean havePrinted = false;

        if ( printRuleSet ) {
            if ( havePrinted )
                System.out.println();
            System.out.println("## Rules");
            ShaclRulesWriter.write(System.out, ruleSet, false);
            havePrinted = true;
        }

        if ( printBaseGraph ) {
            if ( ! data.isEmpty() ) {
                if ( havePrinted )
                    System.out.println();

                System.out.println("## Data graph");
                print(System.out, data);
                System.out.println();
                havePrinted = true;
            }
        }

        if ( printRulesData ) {
            Graph d = ruleSet.getData();
            if ( d != null && !d.isEmpty() ) {
                if ( havePrinted )
                    System.out.println();
                System.out.println("## Ruleset data graph");
                print(System.out, ruleSet.getData());
                havePrinted = true;
            }
        }

        if ( printInfGraph ) {
            if ( havePrinted )
                System.out.println();
            System.out.println("## Inferred");
            print(System.out, accGraph);
            havePrinted = true;
        }

        if ( printOutputGraph ) {
            if ( havePrinted )
                System.out.println();
            System.out.println("## Output graph");
            print(System.out, output);
            havePrinted = true;
        }

        // Exit
        if ( havePrinted )
            System.out.println();
    }

    /**
     * Print a graph.
     */
    public static void print(OutputStream out, Graph graph) {
        IndentedWriter iOut = IndentedWriter.clone(IndentedWriter.stdout);
        iOut.incIndent(2);
//        Graph graph2 = PrefixMappingUtils.graphInUsePrefixMapping(graph);
//        RDFWriter.source(graph2).format(RDFFormat.TURTLE_FLAT).output(System.out);

        StreamRDF stream = new PrintingStreamRDF(iOut, Prefixes.adapt(graph.getPrefixMapping()));
        StreamRDFOps.sendTriplesToStream(graph, stream);
    }

    /**
     * Print a graph in flat, abbreviated triples, but don't print the prefix map
     * Development use.
     */
    public static void printJustTriples(Graph graph) {
        NodeFormatter nt = new NodeFormatterTTL(null, Prefixes.adapt(graph));

        AWriter out = IO.wrapUTF8(System.out);
        graph.find().forEach(t->{
            nt.format(out, t.getSubject());
            out.print(" ");
            nt.format(out, t.getPredicate());
            out.print(" ");
            nt.format(out, t.getObject());
            out.println();
        });
        out.flush();
    }

    private void exec1(String fn) {
        String baseURI = IRILib.filenameToIRI(fn);
        IndentedWriter out = IndentedWriter.stdout;

        try ( InputStream in = IO.openFile(fn) ) {
            RuleSet ruleSet = ShaclRulesParser.parse(in, baseURI);
            //addStandardPrefixes(pmap);
            //RulesWriter.write(out, ruleSet);
        } catch (IOException ex) {
            out.flush();
            IO.exception(ex);
        }
    }

    private static void addStandardPrefixes(PrefixMap prefixMap) {
        /** Update {@link PrefixMap} with the SHACLC standard prefixes */
        prefixMap.add("rdf",  RDF.getURI());
        prefixMap.add("rdfs", RDFS.getURI());
        prefixMap.add("sh",   SHACL.getURI());
        prefixMap.add("xsd",  XSD.getURI());
        prefixMap.add("",  "http://example/");
    }

    @Override
    protected String getCommandName() {
        return "rules_eval";
    }

    @Override
    protected String getSummary() {
        return "RULES";
    }
}

