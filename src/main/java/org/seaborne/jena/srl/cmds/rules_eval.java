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

package org.seaborne.jena.srl.cmds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.cmd.ArgDecl;
import org.apache.jena.cmd.CmdException;
import org.apache.jena.cmd.TerminationException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMemFactory;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.*;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.util.PrefixMappingUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.RulesEngine;
import org.seaborne.jena.srl.SRLParser;
import org.seaborne.jena.srl.SRLWriter;
import org.seaborne.jena.srl.examine.Examine;
import org.seaborne.jena.srl.exec.RuleSetEvaluation;
import org.seaborne.jena.srl.exec.RulesEngineRegistry;
import org.seaborne.jena.srl.lang.parser.ParserLib;
import org.seaborne.jena.srl.lang.parser.SRLParseException;
import org.seaborne.jena.srl.sys.RecursionChecker.RecursionException;
import org.seaborne.jena.srl.sys.Stratification.StratificationException;
import org.seaborne.jena.srl.sys.SysJenaRules;
import org.seaborne.jena.srl.sys.SysSRL;
import org.seaborne.jena.srl.sys.WellFormed.NotWellFormedException;

public class rules_eval extends CmdRules {

    static { JenaSystem.init(); }

    public static void main(String...argv) {
        new rules_eval(argv).mainRun();
    }

    private static ArgDecl argOutput   = new ArgDecl(ArgDecl.HasValue, "output", "out");
    private static ArgDecl argExamine  = new ArgDecl(ArgDecl.NoValue, "examine");
    private static ArgDecl argBase     = new ArgDecl(ArgDecl.HasValue, "base");
//    private static ArgDecl argSyntax   = new ArgDecl(ArgDecl.HasValue, "syntax");

    private String baseIRI             = null;

//    private Lang lang                  = null;
//
//    private boolean printRDF           = false;
//    private boolean printText          = false;
//    private boolean printSRL           = false;

    protected rules_eval(String[] argv) {
        super(argv);
        //super.add(argDeclDebug);
        super.add(argOutput,  "--output=",      "Output formats: RDF(r), SRL(s) (default: SRL)");
        //super.add(argSyntax,  "--syntax=NAME",  "Set syntax (otherwise syntax guessed from file extension)");
        super.add(argBase,    "--base=URI",     "Set the base URI");
    }

    @Override
    protected void processModulesAndArgs() {
         super.processModulesAndArgs();

         if ( super.contains(argExamine) ) {
             Examine.EXAMINE = true;
         }

         if ( super.hasArg(argOutput) ) {
//             printRDF = false;
//             printText = false;
//             printSRL = false;
//
//             // Split values.
//             Function<String, Stream<String>> f = (x) -> {
//                 String[] a = x.split(",");
//                 return Arrays.stream(a);
//             };
//
//             List<String> outputValues = getValues(argOutput).stream()
//                     .flatMap(f)
//                     .map(s->s.toLowerCase())
//                     .toList();
//             List<String> values = new ArrayList<>(outputValues); // Mutable.
//
//             printText = values.remove("text") || values.remove("t");
//             printRDF = values.remove("rdf") || values.remove("r") || values.remove("ttl");
//             printSRL = values.remove("s") || values.remove("srl");
//
//             if ( values.remove("all") || values.remove("a") ) {
//                 printRDF = true;
//                 printText = true;
//                 printSRL = true;
//             }
//         } else {
//             // Default
//             printRDF = false;
//             printSRL = true;
//             printText = false;
         }

//         if ( super.contains(argSyntax) ) {
//             String syntax = super.getValue(argSyntax);
//             Lang lang$ = RDFLanguages.nameToLang(syntax);
//             if ( lang$ == null )
//                 throw new CmdException("Can not detemine the syntax from '" + syntax + "'");
//             this.lang = lang$;
//         }

         if ( super.contains(argBase) ) {
             baseIRI = super.getValue(argBase);
             try {
                 IRIx iri = IRIs.reference(baseIRI);
             } catch (IRIException ex) {
                 throw new CmdException("Bad base IRI: " + baseIRI);
             }
         }

         // rules ruleFile [data]
         // rules --rules F --data F

//         if  (positionals.isEmpty() )
//             // stdin
//             positionals.add("-");
    }


    @Override
    protected void exec() {
        // rules ruleFile [data]
        // rules --rules F --data F

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

            var builder = SRLParser.from(rulesFile);
            if ( baseIRI != null )
                builder.baseURI(baseIRI);
//
//
//            ruleSet = SRLParser.from(rulesFile).baseURI(baseURI)
            ruleSet = builder.parse();
        } catch (SRLParseException ex) {
            throw messageParseErrorAndTerminate(ex, 1);
        }
        Graph data = GraphMemFactory.createDefaultGraph();

        if ( dataFile != null ) {
            RDFParser.source(dataFile).parse(data);
        } else {
            if ( ruleSet.hasPrefixMap() )
                data.getPrefixMapping().setNsPrefixes(Prefixes.adapt(ruleSet.getPrefixMap()));
        }

        boolean verbose = super.isVerbose();
        if ( verbose ) {
            Examine.EXAMINE = true;
        }

        EnumSet<OUTPUT> outputs = EnumSet.noneOf(OUTPUT.class);
        outputs.add(OUTPUT.INFERENCE_GRAPH);
        outputs = EnumSet.copyOf(outputs);

        try {
            RulesEngine engine = defaultRulesEngine(data, ruleSet); //.setTrace(verbose);
            exec(ruleSet, data, engine,outputs);
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

    private static RuntimeException messageParseErrorAndTerminate(SRLParseException ex, int rc) {
        String msg = ParserLib.formatMessage(ex.getMessage(), ex.getLine(), ex.getColumn());
        System.err.print("  ");
        System.err.print(msg);
        System.err.println();
        throw new TerminationException(rc);
    }

    private static RuntimeException messageAndTerminate(RuntimeException ex, int rc) {
        System.err.print("  ");
        System.err.print(ex.getMessage());
        System.err.println();
        throw new TerminationException(rc);
    }


    private static RulesEngine defaultRulesEngine(Graph baseGraph, RuleSet ruleSet) {
        RulesEngine engine = RulesEngineRegistry.get()
                .create(SysJenaRules.dftEngineType, baseGraph, null, ruleSet, SysSRL.getContext());
        return engine;
    }

    enum OUTPUT {
        RULE_SET,
        BASE_GRAPH,
        INFERENCE_GRAPH,    // Includes DATA block
        OUTPUT_GRAPH        // Base graph + Inference graph
    }

    static EnumSet<OUTPUT> defaultPrint = EnumSet.of(OUTPUT.INFERENCE_GRAPH);

    private static void exec(RuleSet ruleSet, Graph baseGraph, RulesEngine engine, EnumSet<OUTPUT> outputs) {
        //boolean printRuleSet = Examine.EXAMINE ||
        boolean printRuleSet = outputs.contains(OUTPUT.RULE_SET);
        boolean printBaseGraph = outputs.contains(OUTPUT.BASE_GRAPH);
        boolean printRulesData = outputs.contains(OUTPUT.BASE_GRAPH);
        boolean printInfGraph = outputs.contains(OUTPUT.INFERENCE_GRAPH);
        boolean printOutputGraph = outputs.contains(OUTPUT.OUTPUT_GRAPH);

        printInfGraph = true;

        boolean havePrinted = false;
        if ( printRuleSet ) {
            if ( havePrinted )
                System.out.println();
            System.out.println("==== Rules");
            SRLWriter.write(System.out, ruleSet, false);
            System.out.println();
            havePrinted = true;
        }

        boolean emptyBaseGraph = (baseGraph == null || baseGraph.isEmpty());

        RuleSetEvaluation e = engine.eval();
//        if ( e.rounds() >= 0 ) {
//            System.out.println("## Rounds: "+e.rounds());
//            System.out.println();
//        }

        // If the base graph is empty, "inferred" and "output" are the same
        if ( emptyBaseGraph && printOutputGraph )
            printInfGraph = false;

        if ( printBaseGraph && ! emptyBaseGraph ) {
            if ( havePrinted )
                System.out.println();

            System.out.println("## Data graph");
            print(System.out, baseGraph);
            havePrinted = true;
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

        if ( printOutputGraph ) {
            if ( havePrinted )
                System.out.println();
            System.out.println("## Output graph");
            print(System.out, e.outputGraph());
            havePrinted = true;
        }

        if ( printInfGraph ) {
            if ( havePrinted )
                System.out.println();
            System.out.println("## Inference graph");
            print(System.out, e.inferredTriples());
            havePrinted = true;
        }

//        // Exit
//        if ( havePrinted )
//            System.out.println();
    }

    /**
     * Print a graph.
     */
    public static void print(OutputStream out, Graph graph) {
        if ( true ) {
          Graph graph2 = PrefixMappingUtils.graphInUsePrefixMapping(graph);
          RDFWriter.source(graph2).format(RDFFormat.TURTLE).output(System.out);
          return;
        }


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
            RuleSet ruleSet = SRLParser.parse(in, baseURI);
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

