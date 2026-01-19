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

import java.io.PrintStream;
import java.util.*;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.ShaclRulesWriter;
import org.seaborne.jena.shacl_rules.lang.RuleElement;

/**
 * Rules dependency graph. The graph has vertices of rules and links being "depends
 * on" another rule, i.e. for a triple that is in the head of a rule, it is the rules
 * that can generate that triple. relations in its body. from this, we can determine
 * whether a rule is:
 * <ul>
 * <li>Data only (body contains relations that only appear in the data)</li>
 * <li>Not-recursive: it can be solved by top-down flattening</li>
 * <li>Mutually recursive rules</li>
 * <li>Linear (only one body relationship is recursive)</li>
 * <li>Depends on a negation or aggregation
 * </ul>
 */

public class DependencyGraphAlt {
    // Does not use "providers" (triples to rule multimap), instead it iterates over the rule set.

    // Edge type.
    enum Link {
        POSITIVE("+"), NEGATIVE("-"), AGGREGATE("A");
        public final String symbol;
        Link(String symbol) { this.symbol = symbol; }
    }

    public record Edge(Rule rule, Link link, Rule linkedRule ) {
        //public boolean edgeToBase() { return linkedRule == null; }
    }

    // Rule -> other rules it directly depends on (no path traversal).
    // See also level0 for rules that only depend on the base graph.
    private ListValuedMap<Rule, Edge> direct = MultiMapUtils.newListValuedHashMap();

    // Rule without any dependent rules (the rule is satisfied by the data directly)
    private Set<Rule> level0 = new HashSet<>();

    private final RuleSet ruleSet;

    public static DependencyGraphAlt create(RuleSet ruleSet) {
        DependencyGraphAlt depGraph = new DependencyGraphAlt(ruleSet);
        //depGraph.initialize();
        return depGraph;
    }

    private DependencyGraphAlt(RuleSet ruleSet) {
        this.ruleSet = ruleSet;

        // For each rule, connect to its positive and negative dependencies.
        ruleSet.getRules().forEach(rule->{
            Collection<Edge> connections = edges(rule, ruleSet);
            if ( connections.isEmpty() ) {
                // Alternative is have a "no edge" distinguished edge.
                // Maybe be necessary for positive and negative flavours.
                level0.add(rule);
            } else {
                direct.putAll(rule, connections);
            }
        });
    }

    // ---- Calculate the direct edge set.

    private static final boolean DEBUG_BUILD = false;

    // Entry point to calculate the direct edge set.
    static Collection<Edge> edges(Rule rule, RuleSet ruleSet) {
        if ( DEBUG_BUILD )
            ShaclRulesWriter.print(rule);
        List<Edge> connections = new ArrayList<>();

        accumulateEdges(connections, rule, Link.POSITIVE, rule.getBodyElements(), ruleSet);

        if ( DEBUG_BUILD )
            System.out.println(connections.size()+" :: put:"+connections);
        return connections;
    }

    private static void accumulateEdges(List<Edge> accumulator, Rule rule, Link linkType, List<RuleElement> elts, RuleSet ruleSet) {
        for ( RuleElement elt : elts ) {
            switch(elt) {
                case RuleElement.EltTriplePattern(Triple triplePattern) -> {
                    accumulateEdgesTriplePattern(accumulator, rule, linkType, triplePattern, ruleSet);
                }
                case RuleElement.EltNegation(List<RuleElement> inner) -> {
                    // Do as a second pass once all the positives are done?
                    // NB Negative overrides positive in stratification.
                    // Anything inside NOT is also "negative"
                    accumulateEdges(accumulator, rule, Link.NEGATIVE, inner, ruleSet);
                }
                // These do not cause a dependency relationship.
//                case RuleElement.EltCondition(Expr condition) -> {}
//                case RuleElement.EltAssignment(Var var, Expr expression) -> {}

                case null -> throw new RulesException("Encountered a null rule element");

                default -> {}
            }
        }
    }

    private static void accumulateEdgesTriplePattern(List<Edge> accumulator, Rule rule, Link linkType, Triple triplePattern, RuleSet ruleSet) {
        // Alt to using providers.
        // Loop on rules and use RuleDependencies.dependsOn(triplePattern, rule) which looks in heads.
        for ( Rule r : ruleSet.getRules() ) {
            if ( RuleDependencies.dependsOn(triplePattern, r) ) {
                if ( freshEdge(accumulator, rule, linkType, r) )
                    accumulator.add(new Edge(rule, linkType, r));
            }
        }
    }

    // Checked whether an edge is already in the collection.
    // XXX id per edge may be sensible.
    private static boolean freshEdge(List<Edge> array, Rule rule, Link linkType, Rule r) {
        for ( Edge e : array ) {
            if ( e.rule.id == rule.id &&
                 e.link == linkType &&
                 e.linkedRule.id == r.id )
                return false;
        }
        return true;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * Return direct dependencies for a rule.
     * This method returns null if the rule is not in the dependency graph
     * and this method returns an empty collection for a rule that only
     * depends on the base graph.
     */
    public Collection<Edge> directDependencies(Rule rule) {
        if ( level0.contains(rule) )
            return List.of();
        return direct.get(rule);
    }

    public void print() { print(IndentedWriter.stdout.clone()); }

    public void print(PrintStream pStream) {
        print(new IndentedWriter(pStream));
    }

    private static int EdgeOffset = 4;

    public void print(IndentedWriter out) {
        //out.setEndOfLineMarker(" NL");

        try ( out ) {
            out.println("[DependencyGraph]");
            if ( ! level0.isEmpty() ) {
                out.println("Level0 ");
                out.incIndent();
                for ( Rule r : level0 ) {
                    // Entries with no dependencies.
                    ShaclRulesWriter.print(out, r, ruleSet.getPrefixMap(), true);
                }
                out.decIndent();
            }
            if ( ! direct.isEmpty() ) {
                out.println("Edges ");
                out.incIndent();
                for ( Rule r : direct.keySet() ) {
                    ShaclRulesWriter.print(out, r, ruleSet.getPrefixMap(), true);
                    out.ensureStartOfLine();
                    Collection<Edge> c = direct.get(r);
                    c.forEach(edge -> {
                        out.incIndent(EdgeOffset);
                        out.print(edge.link.symbol);
                        out.print(" ");
                        ShaclRulesWriter.print(out, edge.linkedRule, ruleSet.getPrefixMap(), true);
                        out.decIndent(EdgeOffset);
                    });
                }
                out.decIndent();
            }
        } finally { out.flush(); }
    }
}
