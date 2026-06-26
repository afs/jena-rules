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

package org.seaborne.jena.shacl_rules.sys;

import java.util.Arrays;
import java.util.Map;

import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;

/**
 * SHACL Rules prefixes
 *
 * See <a href="https://github.com/w3c/data-shapes/issues/496">SHACL namespaces</a>
 */
public class P {

    // XXX Simplify.

    public static final String SH     = "http://www.w3.org/ns/shacl#";
    public static final String SRL    = "http://www.w3.org/ns/shacl-rules#";
    public static final String SHNEX  = "http://www.w3.org/ns/shacl-node-expr#";
    public static final String SPARQL = "http://www.w3.org/ns/sparql#";

    public static final String JenaRulesNS = "http://jena.apache.org/shacl-rules#";
    public static final String JenaRulesSymbolsNS = "http://jena.apache.org/symbols-rules#";

    //@formatter:off
    private static Map<String, String> thePrefixesMap = Map.of("rdf",     RDF.getURI(),
                                                            "sh",      SH,
                                                            "srl",     SRL,
                                                            "shnex",   SHNEX,
                                                            "sparql",  SPARQL,
                                                            "arq",     "http://jena.apache.org/ARQ/function#",
                                                            "arqnx",   "http://jena.apache.org/ARQ/nx#"
                                                            );

    //@formatter:on

    /** Build a PREFIXes block for SRL/Turtle/SPARQL syntax. */
    private static String prefixesAsString(Map<String, String> map, String...includes) {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(includes).forEachOrdered(p->{
            String u = map.get(p);
            if ( u != null ) {
                String line = String.format("PREFIX %-8s <%s>\n", p+":", u);
                sb.append(line);
            } else {
                FmtLog.error(P.class, "No entry for prefix '%s'", p);
            }
        });
        return sb.toString();
    }

    // A string in Turtle format that puts in common prefixes related to rules.
    public static final String PREFIXES = prefixesAsString(thePrefixesMap, "rdf", "sh", "srl", "sparql", "shnex");

    // Convenience.
    private static PrefixMap prefixMap =
            PrefixMapFactory.unmodifiablePrefixMap(PrefixMapFactory.create(thePrefixesMap));

    /** Convenience for rules related prefixes. */
    public static String getPrefix(String prefix) {
        return prefixMap.get(prefix);
    }

    /** Convenience for rules related prefixes. */
    public static String expandPrefix(String prefixedName) {
        return prefixMap.expand(prefixedName);
    }


    /**
     * Add prefixes for {@code srl:} and {@code sparql:}
     * if they aren't defined already.
     */
    public static void basicPrefixes(Graph graph) {
        PrefixMapping pmap = graph.getPrefixMapping();
        addCarefully(pmap, "rdf", RDF.getURI());
        addCarefully(pmap, "srl", SRL);
        addCarefully(pmap, "sparql", SPARQL);
   }

    private static void addCarefully(PrefixMapping pmap, String prefix, String uri) {
        if ( pmap.getNsPrefixURI(prefix) == null && pmap.getNsURIPrefix(uri) == null )
            pmap.setNsPrefix(prefix, uri);
    }
}
