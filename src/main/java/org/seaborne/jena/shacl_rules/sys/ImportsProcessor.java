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

import java.util.*;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Graph;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.irix.IRIs;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.streammgr.StreamManager;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.shacl.sys.ShaclSystem;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.ShaclRules;
import org.seaborne.jena.shacl_rules.rdf_syntax.GraphToRuleSet;
import org.seaborne.jena.shacl_rules.tuples.Tuple;
import org.slf4j.Logger;

/**
 * Import processing for SHACL 1.2 Rules.
 * The result is a collection of RuleSets found in the imports walk.
 */
public class ImportsProcessor {

    public static Logger importsLogger = ShaclSystem.shaclSystemLogger;

    public static final String connegAcceptHeader = ShaclRules.mtShapeRuleLanguage+","+WebContent.defaultGraphAcceptHeader;

    public static ImportsProcessor create() {
        return new ImportsProcessor(null);
    }

    public static RuleSet mergeClosure(RuleSet ruleSet) {
        List<RuleSet> rulesets = create().loadImports(ruleSet);

        PrefixMap prefixMap = PrefixMapFactory.create(ruleSet.getPrefixMap());
        Graph mergedData = GraphFactory.createDefaultGraph();
        G.addInto(mergedData, ruleSet.getData());
        List<Rule> rules = new ArrayList<>(ruleSet.getRules());
        Set<Tuple> tuples = new LinkedHashSet<>(ruleSet.getDataTuples());

        G.addInto(mergedData, ruleSet.getData());
        for ( RuleSet rs : rulesets ) {
            prefixMap.putAll(rs.getPrefixMap());
            rules.addAll(rs.getRules());
            G.addInto(mergedData, rs.getData());
            rs.getTuplesData();
        }

        List<Tuple> listTuples = tuples.stream().toList();
        RuleSet result =
            RuleSet.create(ruleSet.getBase(),
                           prefixMap,
                           Set.of(),
                           rules,
                           mergedData.find().toList(),
                           listTuples);
        return result;
    }


    // Function to find a node in graph to start from.
//    private final Supplier<Node> startPoint;
//    private final Node importsProperty;
    private final StreamManager streamManager;

    private ImportsProcessor(StreamManager streamManager) {
        if ( streamManager == null )
            streamManager = new StreamManager();
        this.streamManager = streamManager;
        // Function to find the starting point,
        // Null means any importsProperty triple.
        //this.startPoint = ()->null;
    }

    public List<RuleSet> loadImports(RuleSet ruleSet) {
        return loadImports(ruleSet, null);
    }

    public List<RuleSet> loadImports(RuleSet ruleSet, String topURL) {
        if ( !ruleSet.hasImports() )
            return List.of();
        List<RuleSet> acc = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        if ( topURL != null ) {
            String url = IRIs.resolve(topURL);
            visited.add(topURL);
        }
        try {
            for ( String importURL : ruleSet.getImports() ) {
                loadVisitImports(importURL, acc, visited);
            }
        } catch (HttpException ex) {
            throw new ShaclException("Exception during imports processing", ex);
        }
        return acc;
    }

    /**
     * Load from a URL or file, RDF or SRL syntax, process any imports in a nested fashion.
     */
    private void loadVisitImports(String url, List<RuleSet> acc, Set<String> visited) {
        url = IRIs.resolve(url);
        if ( visited.contains(url) )
            return;
        visited.add(url);
        RuleSet ruleSet = load1(url);
        acc.add(ruleSet);
        processImports(ruleSet, acc, visited);
    }

    // Process imports of a ruleset, recursively.
    private void processImports(RuleSet ruleSet,  List<RuleSet> acc, Set<String> visited) {
        List<RuleSet> importedRuleSets = new ArrayList<>();
        for ( String importURL : ruleSet.getImports() ) {
            loadVisitImports(importURL, acc, visited);
        }
    }

    private RuleSet load1(String url) {
        Objects.requireNonNull(url);
        url = IRIs.resolve(url);
        String scheme = IRIs.scheme(url);
        if ( "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) )
            return loadHttp1(url);
        return ShaclRules.parseFile(url);
    }


    private RuleSet loadHttp1(String url) {
        TypedInputStream input;
        try {
            input = ImportsLib.openTypedInputStream(HttpEnv.getHttpClient(url),
                                                    url, null, streamManager,
                                                    connegAcceptHeader, null);
        } catch (HttpException ex) {
            throw new ShaclException("Exception during imports processing: "+url, ex);
        }
        String contentTypeStr = input.getContentType();

        if ( ShaclRules.mtShapeRuleLanguage.equalsIgnoreCase(contentTypeStr) ) {
            return ShaclRules.parse(input, url);
        }
        // RDF or error
        ContentType contentType = WebContent.determineCT(input.getContentType(), null, url);
        Lang lang = RDFLanguages.contentTypeToLang(contentType);
        if ( lang == null )
            throw new ShaclException("Can not detemine the content type from '"+contentTypeStr+"'");
        try {
            Graph graph = RDFParser.source(input).lang(lang).toGraph();
            RuleSet ruleSet = GraphToRuleSet.parse(graph);
            return ruleSet;
        } catch (RiotException | HttpException ex) {
            throw new ShaclException("Can not load <"+url+">", ex);
        } catch (ShaclException ex) {
            throw ex;
        }
    }
}
