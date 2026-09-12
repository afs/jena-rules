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

package org.seaborne.jena.srl.junit;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.arq.junit.EarlReport;
import org.apache.jena.arq.junit.manifest.TestMakers;
import org.apache.jena.arq.junit.textrunner.TextTestRunner;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.apache.jena.sparql.vocabulary.DOAP;
import org.apache.jena.sparql.vocabulary.EARL;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

public class rulestests {

    // See rdftests
    public static void main(String... args) {
        final PrintStream earlOut = System.out;
        TestMakers.install(RuleTests::makeRuleTest);

        List<String> manifests = Arrays.asList(args);
        boolean createEarlReport = ( manifests.contains("--earl") || manifests.contains("-earl") );
        if ( ! createEarlReport ) {
            TextTestRunner.run(manifests);
            System.exit(0);
        }

        // Make it mutable.
        manifests = new ArrayList<>(manifests);
        manifests.remove("--earl");
        manifests.remove("-earl");

        EarlReport earlReport = new EarlReport(systemURI);
        // Adjust prefixes.
        /*

        earl.setNsPrefix("earl", EARL.getURI());
        earl.setNsPrefix("foaf", FOAF.getURI());
        earl.setNsPrefix("rdf", RDF.getURI());
        earl.setNsPrefix("dc", DC.getURI());
        earl.setNsPrefix("dct", DCTerms.getURI());
        earl.setNsPrefix("doap", DOAP.getURI());
        earl.setNsPrefix("xsd", XSD.getURI());
        earl.setNsPrefix("rdft", "http://www.w3.org/ns/rdftest#");
         */


        TextTestRunner.run(earlReport, manifests);

        earlOut.println();
        earlOut.println("# SPARQL_RL EARL Report");
        Model model = earlReport.getModel();

        RDFDataMgr.write(earlOut, model, Lang.TURTLE);

        earlOut.println();

        Model meta = metadata();
        RDFDataMgr.write(earlOut, meta, Lang.TURTLE);

//            // ---
//            Model meta = metadata(earlReport);
            // Write meta separately so it is easy to find and can be extracted.
//            RDFDataMgr.write(earlOut, model, Lang.TURTLE);
//            earlOut.println();
//            RDFDataMgr.write(earlOut, meta, Lang.TURTLE);
    }

    private static String homepageStr = "https://github.com/afs/jena-rules/";
    private static String name = "SPARQL-RL for Apache Jena";
    private static String releaseVersion = "1.0";
    private static String systemURI = "https://github.com/afs/jena-rules/";

    // Generate metadata into a separate model. Does not update the report.
    // Should have a subset of the EARL report prefixes.
    private static Model metadata() {
        // Convert to a """ block.
        Model model = ModelFactory.createDefaultModel();
        Resource homepage = model.createResource(homepageStr);

        model.setNsPrefix("rdf", RDF.getURI()) ;
        //model.setNsPrefix("rdfs", RDFS.getURI()) ;
        model.setNsPrefix("earl", EARL.getURI()) ;
        model.setNsPrefix("foaf", FOAF.getURI()) ;
        model.setNsPrefix("doap", DOAP.getURI()) ;
        model.setNsPrefix("xsd", XSD.getURI()) ;
        model.setNsPrefix("rdft", "http://www.w3.org/ns/rdftest#");
        model.setNsPrefix("dc", DC.getURI());

        Resource system = systemURI == null ? model.createResource() : model.createResource(systemURI);

        if ( name != null )
            model.add(system, DC.title, name);

        Resource who = model.createResource(FOAF.Agent)
                    .addProperty(FOAF.name, "Andy Seaborne")
                    //.addProperty(FOAF.name, "Apache Jena Community")
                    //.addProperty(FOAF.homepage, homepage)
                    ;

        model.add(system, DC.creator, who);
        model.add(system, RDF.type, DOAP.Project);
        model.add(system, DOAP.name, name);
        model.add(system, DOAP.homepage, homepage);
        model.add(system, DOAP.developer, who);
        model.add(system, DOAP.maintainer, who);
        model.add(system, DOAP.shortdesc,  model.createLiteral("RDF and SPARQL triple store", "en"));
        model.add(system, DOAP.description, model.createLiteral("Apache Jena : RDF system and SPARQL triple store", "en"));

        Resource release = model.createResource(DOAP.Version);
        model.add(system, DOAP.release, release);

//        GregorianCalendar gCal = new GregorianCalendar();
//        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        ZonedDateTime zdt = gCal.toZonedDateTime();
//        String lex = fmt.format(zdt) ;

        Node today_node = NodeFactoryExtra.todayAsDate();
        Literal today = model.createTypedLiteral(today_node.getLiteralLexicalForm(), today_node.getLiteralDatatype());
        model.add(release, DOAP.created, today);
        model.add(release, DOAP.revision, releaseVersion);
        model.add(release, DOAP.homepage, homepage);
        return model;
    }

}

