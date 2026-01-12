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

package org.seaborne.jena.shacl_rules.sys;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.irix.IRIs;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.shared.JenaException;

/**
 * SHACL Rules prefixes
 *
 * See <a href="https://github.com/w3c/data-shapes/issues/496">SHACL namespaces</a>
 */
public class P {

    public static final String SH = "http://www.w3.org/ns/shacl#";
    public static final String SHR = "http://www.w3.org/ns/shacl-rules#";
    public static final String SHNEX = "http://www.w3.org/ns/shacl-node-expr#";


    /** Build a PREFIXes block for SRL/Turtle/SPARQL syntax. */
    private static String prefixesAsString(Map<String, String> map, String...includes) {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(includes).forEachOrdered(p->{
            String u = map.get(p);
            if ( u != null ) {
                String line = String.format("PREFIX %-8s <%s>\n", p+":", u);
                sb.append(line);
            }
        });
        return sb.toString();


    }

    //@formatter:off
    private static Map<String, String> prefixesMap = Map.of("rdf",     "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                                                            "sh",      SH,
                                                            "shr",     SHR,
                                                            "shnex",   SHNEX,
                                                            "sparql",  "http://www.w3.org/ns/sparql#",
                                                            "arq",     "http://jena.apache.org/ARQ/function#");

    //@formatter:on

    public static final String PREFIXES = prefixesAsString(prefixesMap, "rdf", "sh", "shr", "shnex", "sparql", "arq");


    // It would be nice if this were immutable.
    public static PrefixMap prefixMap = PrefixMapFactory.create(prefixesMap);

    public static String getPrefix(String prefix) {
        return prefixMap.get(prefix);
    }

    public static void addPrefixes(Graph graph) {
        prefixesMap.forEach((prefix, uri) -> graph.getPrefixMapping().setNsPrefix(prefix, uri));

//        JLib.addPrefixes(graph,
//                         "rdf",     "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
//                         "sh",      "http://www.w3.org/ns/shacl#",
//                         "srl",     "http://www.w3.org/ns/shacl-rules#",
//                         "shnex",   "http://www.w3.org/ns/shnex#",
//                         "sparql:", "http://www.w3.org/ns/sparql#");
    }

    /**
     * Add prefixes written as pairs of strings.
     * <p>
     * The number of strings must be even. The first of a pair is the prefix, the second is the URI.
     */
    public static void addPrefixes(Graph graph, String... str) {
        if ( (str.length & 1) != 0 )
            throw new JenaException("Must be an even number or string arguments");
        Map<String, String> x = new HashMap<>();
        for (int i = 0 ; i < str.length ; i += 2 ) {
            addPrefix(graph, str[i], str[i+1]);
        }
        graph.getPrefixMapping().setNsPrefixes(x);
    }

    /**
     * Add a prefix to a graph.
     * <p>
     * The number of strings must be even. The first of a pair is the prefix, the second is the URI.
     */
    public static void addPrefix(Graph graph, String prefix, String uri) {
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(uri);
        if ( prefix.endsWith(":") )
            prefix = StringUtils.chop(prefix);
        IRIs.checkEx(uri);
        graph.getPrefixMapping().setNsPrefix(prefix, uri);
    }
}
