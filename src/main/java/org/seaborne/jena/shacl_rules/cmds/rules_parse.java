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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.cmd.ArgDecl;
import org.apache.jena.cmd.CmdException;
import org.apache.jena.graph.Graph;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sys.JenaSystem;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.ShaclRulesWriter;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParserBase;
import org.seaborne.jena.shacl_rules.rdf_syntax.GraphToRuleSet;
import org.seaborne.jena.shacl_rules.rdf_syntax.RuleSetToGraph;
import org.seaborne.jena.shacl_rules.sys.P;

public class rules_parse extends CmdRules {

    static { JenaSystem.init(); }

    private final ArgDecl argDeclDebug       = new ArgDecl(false, "debug");

    private static ArgDecl argOutput   = new ArgDecl(ArgDecl.HasValue, "output", "out");
    private static ArgDecl argBase     = new ArgDecl(ArgDecl.HasValue, "base");
    private static ArgDecl argSyntax   = new ArgDecl(ArgDecl.HasValue, "syntax");

    private static boolean debug = false;


    private String baseIRI             = null;
    private Lang lang                  = null;

    private boolean printRDF           = false;
    private boolean printText          = false;
    private boolean printSRL           = false;

    public static void main(String...argv) {
        new rules_parse(argv).mainRun();
    }

    protected rules_parse(String[] argv) {
        super(argv);
        //super.add(argDeclDebug);
        super.add(argOutput,  "--output=",      "Output formats: RDF(r), SRL(s) (default: SRL)");
        super.add(argSyntax,  "--syntax=NAME",  "Set syntax (otherwise syntax guessed from file extension)");
        super.add(argBase,    "--base=URI",     "Set the base URI");
    }

    @Override
    protected void processModulesAndArgs() {
         super.processModulesAndArgs();

         debug = this.contains(argDeclDebug);

         if ( super.hasArg(argOutput) ) {
             printRDF = false;
             printText = false;
             printSRL = false;

             // Split values.
             Function<String, Stream<String>> f = (x) -> {
                 String[] a = x.split(",");
                 return Arrays.stream(a);
             };

             List<String> outputValues = getValues(argOutput).stream()
                     .flatMap(f)
                     .map(s->s.toLowerCase())
                     .toList();
             List<String> values = new ArrayList<>(outputValues); // Mutable.

             printText = values.remove("text") || values.remove("t");
             printRDF = values.remove("rdf") || values.remove("r") || values.remove("ttl");
             printSRL = values.remove("s") || values.remove("srl");

             if ( values.remove("all") || values.remove("a") ) {
                 printRDF = true;
                 printText = true;
                 printSRL = true;
             }
         } else {
             // Default
             printRDF = false;
             printSRL = true;
             printText = false;
         }

         if ( super.contains(argSyntax) ) {
             String syntax = super.getValue(argSyntax);
             Lang lang$ = RDFLanguages.nameToLang(syntax);
             if ( lang$ == null )
                 throw new CmdException("Can not detemine the syntax from '" + syntax + "'");
             this.lang = lang$;
         }

         if ( super.contains(argBase) ) {
             baseIRI = super.getValue(argBase);
             try {
                 IRIx iri = IRIs.reference(baseIRI);
             } catch (IRIException ex) {
                 throw new CmdException("Bad base IRI: " + baseIRI);
             }
         }

         if  (positionals.isEmpty() )
             // stdin
             positionals.add("-");
    }

    @Override
    protected void exec() {
        boolean filesOK = true;
        for ( String fn : positionals ) {
            if ( ! "-".equals(fn) ) {
                if ( ! IO.exists(fn) ) {
                    System.err.println("File not found: "+fn);
                    filesOK = false;
                }
            }
        }

        if ( debug )
            ShaclRulesParserBase.debug(true);

        if ( ! filesOK )
            throw new CmdException("File(s) not found");

        boolean multipleFiles = (positionals.size() > 1) ;
        boolean first = true;
        for ( String fn: positionals) {
            if ( !first )
                System.out.println();
            first = false;
            exec(fn, multipleFiles);
        }
    }

    private void exec(String rulesFile, boolean multipleFiles) {
        // Bug in ModGeneral - ignores --debug

        RuleSet ruleSet;

        try {
            ruleSet = ShaclRulesParser.parseFile(rulesFile);
            if ( debug )
                System.out.println();
        } catch ( ShaclRulesParseException parseEx) {
            if ( multipleFiles )
                System.err.println(rulesFile+" : ");
            System.out.println("** Parse error");
            return;
        }

        if ( printText ) {
            ShaclRulesWriter.print(ruleSet);
            if ( printRDF || printSRL )
                System.out.println("- - - -");
        }

        if ( printSRL ) {
            ShaclRulesWriter.print(ruleSet);
            if ( printRDF )
                System.out.println("- - - -");
        }

        Graph graph = RuleSetToGraph.asGraph(ruleSet);
        addPrefixes(graph);

        if ( printRDF ) {
            RDFWriter.source(graph).format(RDFFormat.TURTLE_LONG).output(System.out);
        }

        boolean RTT = false;
        if ( RTT ) {
            RuleSet ruleSet2 = GraphToRuleSet.parse(graph);
            if ( true ) {
                //  print text
                ShaclRulesWriter.print(ruleSet2);
                System.out.println("- - - -");
            }

        }
    }

    public static void addPrefixes(Graph graph) { P.addPrefixes(graph); }

    @Override
    protected String getCommandName() {
        return "rules_parse";
    }

    @Override
    protected String getSummary() {
        return "PARSE";
    }
}

