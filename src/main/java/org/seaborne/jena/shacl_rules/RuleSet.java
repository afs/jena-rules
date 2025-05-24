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

package org.seaborne.jena.shacl_rules;

import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.IsoMatcher;

public class RuleSet {

    private final IRIx base;
    private final PrefixMap prefixMap;
    private final List<Rule> rules;
    private final List<Triple> dataTriples;
    private final Graph data;

    public static boolean equalRuleSets(RuleSet ruleSet1, RuleSet ruleSet2) {
//        ruleSet1.getDataTriples();
//        ruleSet1.getPrologue();

        List<Rule> r1 = ruleSet1.getRules();
        List<Rule> r2 = ruleSet2.getRules();
        if ( ! ListUtils.equalsUnordered(r1, r2) )
            return false;

        Graph d1 = ruleSet1.getData();
        Graph d2 = ruleSet2.getData();
        if ( IsoMatcher.isomorphic(d1, d2) )
            return false;
        return true;
    }

    public RuleSet(IRIx base, PrefixMap prefixMap, List<Rule> rules, List<Triple> dataTriples) {
        this.base = base;
        this.prefixMap = Objects.requireNonNull(prefixMap);
        this.rules = Objects.requireNonNull(rules);
        this.dataTriples = dataTriples;

        Graph graph = null;
        if ( dataTriples != null && ! dataTriples.isEmpty() ) {
            graph = GraphFactory.createDefaultGraph();
            GraphUtil.add(graph, dataTriples);
            if ( prefixMap != null ) {
                graph.getPrefixMapping().setNsPrefixes(Prefixes.adapt(prefixMap));
            }
        }
        this.data = graph;
    }

    public PrefixMap getPrefixMap() {
        return prefixMap;
    }

    public boolean hasPrefixMap() {
        if ( prefixMap == null )
            return false;
        return ! prefixMap.isEmpty();
    }

    /**
     * Return the base URI explicitly declared during parsing, if any.
     * This may be null.
     */
    public IRIx getBase() {
        return base;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public Graph getData() {
        return data;
    }

    public List<Triple> getDataTriples() {
        return dataTriples;
    }

    public boolean hasData() {
        if ( dataTriples == null )
            return false;
        return ! dataTriples.isEmpty();
    }

    @Override
    public String toString() {
        return rules.toString();
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }
}
