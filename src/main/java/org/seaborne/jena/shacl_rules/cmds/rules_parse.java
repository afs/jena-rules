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

import org.apache.jena.cmd.CmdException;
import org.apache.jena.cmd.CmdGeneral;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sys.JenaSystem;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.rdf_syntax.RuleSetToTriples;
import org.seaborne.jena.shacl_rules.writer.ShaclRulesWriter;

public class rules_parse extends CmdGeneral {

    static { JenaSystem.init(); }

    public static void main(String...argv) {
        new rules_parse(argv).mainRun();
    }

    protected rules_parse(String[] argv) {
        super(argv);
    }

    @Override
    protected void exec() {
        //rules ruleFile [data]


        if ( positionals.isEmpty() ) {
            throw new CmdException("Usage: rules RulesFile ...");
        }

        boolean first = true;
        for (String rulesFile : positionals) {
            if ( first )
                System.out.println();
            first = false;

            RuleSet ruleSet = ShaclRulesParser.parseFile(rulesFile);

            ShaclRulesWriter.print(ruleSet);

            System.out.println("- - - -");

            Graph graph = RuleSetToTriples.write(ruleSet);
            addPrefixes(graph);

            RDFWriter.source(graph).format(RDFFormat.TURTLE_LONG).output(System.out);
        }
    }

    public static void addPrefixes(Graph graph) {
        JLib.addPrefixes(graph,
                         "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                         "sh", "http://www.w3.org/ns/shacl#",
                         "sparql:", "http://www.w3.org/ns/sparql#"
                         );
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

