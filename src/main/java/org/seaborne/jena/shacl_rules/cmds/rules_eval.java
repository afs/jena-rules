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

package org.seaborne.jena.shacl_rules.cmds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.cmd.CmdException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMemFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.util.PrefixMappingUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesEngine;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.ShaclRulesWriter;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.exec.RulesEngineFwdSimple;

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
            }
            case 2 -> {
                rulesFile = positionals.get(0);
                dataFile = positionals.get(1);
            }
            default ->
                throw new CmdException("Usage: rules exec RulesFile [DataFile]");
        }

        RuleSet ruleSet = ShaclRulesParser.parseFile(rulesFile);
        Graph data = GraphMemFactory.createDefaultGraph();

        if ( dataFile != null ) {
            RDFParser.source(dataFile).parse(data);
        } else {
            if ( ruleSet.hasPrefixMap() )
                data.getPrefixMapping().setNsPrefixes(Prefixes.adapt(ruleSet.getPrefixMap()));
        }

        boolean verbose = super.isVerbose();
        RulesEngine engine = defaultRulesEngine(data, ruleSet).setTrace(verbose);
        exec(ruleSet, data, engine);
    }

    private static RulesEngine defaultRulesEngine(Graph data, RuleSet ruleSet) {
        RulesEngine engine = RulesEngineFwdSimple.build(data, ruleSet);
        return engine;
    }

    public static void exec(RuleSet ruleSet, Graph data, RulesEngine engine) {
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

        if ( printRuleSet ) {
            System.out.println("## Rules");
            ShaclRulesWriter.print(System.out, ruleSet, false);
        }

        if ( printBaseGraph ) {
            if ( ! data.isEmpty() ) {
                System.out.println("## Data graph");
                print(System.out, data);
                System.out.println();
            }
        }

        if ( printRulesData ) {
            Graph d = ruleSet.getData();
            if ( d != null && !d.isEmpty() ) {
                System.out.println("## Ruleset data graph");
                print(System.out, ruleSet.getData());
                System.out.println();
            }
        }

        if ( printInfGraph ) {
            System.out.println("## Inferred");
            print(System.out, accGraph);
            System.out.println();
        }

        if ( printOutputGraph ) {
            System.out.println("## Output graph");
            print(System.out, output);
            System.out.println();
        }
    }

    /**
     * Print a graph.
     */
    public static void print(OutputStream out, Graph graph) {
        Graph graph2 = PrefixMappingUtils.graphInUsePrefixMapping(graph);
        RDFWriter.source(graph2).format(RDFFormat.TURTLE_BLOCKS).output(System.out);
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

