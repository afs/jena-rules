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
import org.apache.jena.arq.junit.riot.VocabLangRDF;
import org.apache.jena.arq.junit.textrunner.TextTestRunner;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.TestManifest;

public class rulestests {



    // See rdftests
    public static void main(String[] args) {
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

        EarlReport earlReport = new EarlReport(null/*bnode*/);
        TextTestRunner.run(earlReport, manifests);
        earlOut.println();
        earlOut.println("# SPARQL_RL EARL Report");
        Model model = earlReport.getModel();

        // Lang
        model.setNsPrefix("rdft", VocabLangRDF.getURI()) ;
        // SPARQL
        model.setNsPrefix("dawg", TestManifest.getURI()) ;
        RDFDataMgr.write(earlOut, model, Lang.TURTLE);

//            // ---
//            Model meta = metadata(earlReport);
            // Write meta separately so it is easy to find and can be extracted.
//            RDFDataMgr.write(earlOut, model, Lang.TURTLE);
//            earlOut.println();
//            RDFDataMgr.write(earlOut, meta, Lang.TURTLE);
    }
}

