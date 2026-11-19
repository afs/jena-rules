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

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.cmd.ArgDecl;
import org.apache.jena.cmd.CmdException;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIx;
import org.apache.jena.query.Query;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.graph.PrefixMappingAdapter;
import org.apache.jena.sparql.modify.request.UpdateWriter;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.update.Update;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.SRLParser;
import org.seaborne.jena.srl.exec.RulesLibSparql;
import org.seaborne.jena.srl.lang.parser.RulesParserBase;
import org.seaborne.jena.srl.lang.parser.SRLParseException;

/** Parse an SRL file/string and write out SPARQL */
public class rules_translate extends CmdRules {

    static { JenaSystem.init(); }

    private final ArgDecl argDeclDebug       = new ArgDecl(false, "debug");

    private static ArgDecl argOutput        = new ArgDecl(ArgDecl.HasValue, "output", "out");
    private static ArgDecl argBase          = new ArgDecl(ArgDecl.HasValue, "base");
    private static ArgDecl argSyntax        = new ArgDecl(ArgDecl.HasValue, "syntax");

    // Translate to SELECT *
    private static ArgDecl argBody          = new ArgDecl(ArgDecl.NoValue, "body");
    // CONSTRUCT/INSERT/
    private static ArgDecl argQueryForm     = new ArgDecl(ArgDecl.HasValue, "form", "type");

    private static boolean debug = false;

    private String baseIRI             = null;
    private Lang lang                  = null;
    private OUTPUT output              = null;

    public static void main(String...argv) {
        new rules_translate(argv).mainRun();
    }

    enum OUTPUT { CONSTRUCT, INSERT, SELECT }

    protected rules_translate(String[] argv) {
        super(argv);
        //super.add(argDeclDebug);
        super.add(argOutput,    "--output=",      "Output formats: CONSTRUCT(c), INSERT(i), SELECT(s)");
        super.add(argSyntax,  	"--syntax=NAME",  "Set syntax (otherwise syntax guessed from file extension)");
        super.add(argBase,    	"--base=URI",     "Set the base URI");
        super.add(argBody,    	"--body",         "Output the body as SELECT *");
    }

    @Override
    protected void processModulesAndArgs() {
         super.processModulesAndArgs();

         debug = this.contains(argDeclDebug);

         output = OUTPUT.SELECT;

         // XXX enum for output type
         if ( super.contains(argBody) ) {
             System.err.println("Not implemented: \"--body\"");
             System.exit(1);
             output = OUTPUT.SELECT;
         }

         if ( super.contains(argOutput) ) {
             String outStr = super.getValue(argOutput);
             outStr = Lib.lowercase(outStr);
             output = switch(outStr) {
                 case "construct", "c" -> OUTPUT.CONSTRUCT;
                 case "insert", "i" -> OUTPUT.INSERT;
                 case "select", "s" ->  OUTPUT.SELECT;
                 default -> {
                     throw new CmdException("Not a recognized output form: '"+outStr+"'");
                 }
             };
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
            RulesParserBase.debug(true);

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
            ruleSet = SRLParser.parseFile(rulesFile);
            if ( debug )
                System.out.println();
        } catch ( SRLParseException parseEx) {
            if ( multipleFiles )
                System.err.println(rulesFile+" : ");
            System.out.println("** Parse error");
            return;
        }

        PrefixMap pmap = ruleSet.getPrefixMap();
        // output
        boolean first = true;
        for (Rule rule : ruleSet.getRules() ) {
            if ( ! first )
                System.out.println();
            first = false;

            switch (output) {
                case SELECT -> printQuery(RulesLibSparql.ruleBodyToSelectQuery(rule.getBody()), pmap);
                case CONSTRUCT -> printQuery(RulesLibSparql.ruleToConstruct(rule), pmap);
                case INSERT -> printUpdate(RulesLibSparql.ruleToInsert(rule), pmap);
            };
        }
    }

    private static void printQuery(Query query, PrefixMap pmap) {
        query.getPrefixMapping().setNsPrefixes(pmap.getMapping());
        IndentedWriter out = IndentedWriter.stdout.clone();
        out.setLineNumbers(true);
        query.serialize(out);
    }

    private static void printUpdate(Update update, PrefixMap pmap) {
        //update.getPrefixMapping().setNsPrefixes(pmap.getMapping());
        IndentedWriter out = IndentedWriter.stdout.clone();
        out.setLineNumbers(true);
        SerializationContext sCxt = new SerializationContext(new PrefixMappingAdapter(pmap));
        UpdateWriter.output(update, out, sCxt);
        out.flush();
    }


    @Override
    protected String getCommandName() {
        return "rules_parse";
    }

    @Override
    protected String getSummary() {
        return "PARSE";
    }
}

