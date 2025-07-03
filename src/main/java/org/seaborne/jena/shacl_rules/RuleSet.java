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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.lib.InternalErrorException;
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

    /**
     * Are two rule sets1 equivalent for execution purposes?
     */
    static boolean equivalentRuleSets(RuleSet ruleSet1, RuleSet ruleSet2) {
        Objects.requireNonNull(ruleSet1);
        Objects.requireNonNull(ruleSet2);
        if ( ruleSet1 == ruleSet2 )
            return true;

        List<Rule> r1 = ruleSet1.getRules();
        List<Rule> r2 = ruleSet2.getRules();

        if ( r1.size() != r2.size () )
            return false;
        int N = r1.size();

        // Copy.
        List<Rule> checked = new ArrayList<>(r2);

        for ( Rule rule1 : r1 ) {
            boolean matched = false;
            for ( Rule rule2 : checked ) {
                if ( rule1.equivalent(rule2) ) {
                    // Match for rule2.
                    // Remove so it does not get matched again.
                    checked.remove(rule2);
                    matched = true;
                    break;
                }
            }
            // Did not match rule1 => false
            if ( ! matched )
                return false;
        }
        // matched checked should be empty.
        if ( !checked.isEmpty() )
            throw new InternalErrorException("Expected 'checked' to be empty");

        // Now check data.
        Graph d1 = ruleSet1.getData();
        Graph d2 = ruleSet2.getData();
        if ( d1 == null && d2 == null )
            return true;
        if ( d1 == null || d2 == null )
            return false;
        if ( ! IsoMatcher.isomorphic(d1, d2) )
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

//    @Override
//    public Iterator<Rule> iterator() {
//        return rules.iterator();
//    }

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

    public int numRules() {
        return rules.size();
    }

    @Override
    public String toString() {
        return rules.toString();
    }

    /** Any rules? */
    public boolean isEmpty() {
        return rules.isEmpty();
    }
}
