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
import java.util.function.Consumer;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFmtLib;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.ShaclRulesWriter;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;

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

public class DependencyGraph {

    // Edge type.
    public enum Link {
        POSITIVE("+"), NEGATIVE("-"), AGGREGATE("A");
        public final String symbol;
        Link(String symbol) { this.symbol = symbol; }
    }

    // XXX Rename as "DependencyEdge"?
    public record Edge(Rule rule, Link link, Rule linkedRule ) {
        //public boolean edgeToBase() { return linkedRule == null; }
    }

    // Rule -> other rules it directly depends on (no path traversal).
    // See also level0 for rules that only depend on the base graph.
    private ListValuedMap<Rule, Edge> direct = MultiMapUtils.newListValuedHashMap();

//    // TEMP
//    // Convenience - does not record whether a positive or a negative edge.
//    private MultiValuedMap<Rule, Rule> directRuleMaker() {
//        MultiValuedMap<Rule, Rule> x = MultiMapUtils.newListValuedHashMap();
//        direct.entries().forEach(entry->x.put(entry.getKey(), entry.getValue().linkedRule));
//        return x;
//    }

    // Rules without any dependent rules (the rule is satisfied by the data directly)
    private Set<Rule> level0 = new HashSet<>();

    private final RuleSet ruleSet;

    public static DependencyGraph create(RuleSet ruleSet) {
        DependencyGraph depGraph = new DependencyGraph(ruleSet);
        //depGraph.initialize();
        return depGraph;
    }

    private DependencyGraph(RuleSet ruleSet) {
        this.ruleSet = ruleSet;

        // Head triple template to rule.
        // Keep?
        MultiValuedMap<Triple, Rule> providers = MultiMapUtils.newListValuedHashMap();
        ruleSet.getRules().forEach(rule->{
            rule.getTripleTemplates().forEach(t->providers.put(t, rule));
        });

        // For each rule, connect to its positive and negative dependencies.
        ruleSet.getRules().forEach(rule->{
            Collection<Edge> connections = edges(rule, providers);
            if ( connections.isEmpty() ) {
                // Alternative is have a "no edge" distinguished edge.
                // May be necessary for positive and negative flavours.
                level0.add(rule);
            } else {
                direct.putAll(rule, connections);
            }
        });
    }

    // ---- Calculate the direct edge set.

    private static final boolean DEBUG_BUILD = false;

    // Entry point to calculate the direct edge set.
    static Collection<Edge> edges(Rule rule, MultiValuedMap<Triple, Rule> providers) {
        if ( DEBUG_BUILD )
            ShaclRulesWriter.print(rule);
        List<Edge> connections = new ArrayList<>();
        accumulateEdges(connections, rule, Link.POSITIVE, rule.getBodyElements(), providers);
        if ( DEBUG_BUILD )
            System.out.println(connections.size()+" :: put:"+connections);
        return connections;
    }

    private static void accumulateEdges(List<Edge> accumulator, Rule rule, Link linkType, List<RuleBodyElement> elts, MultiValuedMap<Triple, Rule> providers) {
        for ( RuleBodyElement elt : elts ) {
            switch(elt) {
                case EltTriplePattern(Triple triplePattern) -> {
                    // Using providers.
                    providers.keySet().forEach(tripleTemplate -> {
                        if ( DEBUG_BUILD ) {
                            System.out.println("Link type: "+linkType);
                            System.out.println("Pattern:   "+NodeFmtLib.displayStr(triplePattern));
                            System.out.println("Template:  "+NodeFmtLib.displayStr(tripleTemplate));
                            System.out.println(RuleDependencies.dependsOn(triplePattern, tripleTemplate));
                        }

                        // Possible improvement.
                        // Find triple templtaes that match riple patterns using e.g. index by predicate.
                        // rather then a ruleset scan?
                        // ie. a better Triple template to rule lookup.
                        //  TriplePattern -> Possible (Triple templates, rule)

                        if ( RuleDependencies.dependsOn(triplePattern, tripleTemplate) ) {
                            providers.get(tripleTemplate).forEach(r -> {
                                // Check for duplicates.
                                if ( freshEdge(accumulator, rule, linkType, r) )
                                    accumulator.add(new Edge(rule, linkType, r));
                            });
                        }
                    });
                }
                case EltNegation(List<RuleBodyElement> inner) -> {
                    // Do as a second pass once all the positives are done?
                    // NB Negative overrides positive in stratification.
                    // Anything inside NOT is also "negative"
                    accumulateEdges(accumulator, rule, Link.NEGATIVE, inner, providers);
                }
                // These do not cause a dependency relationship.
//                case EltCondition(Expr condition) -> {}
//                case EltAssignment(Var var, Expr expression) -> {}

                case null -> throw new RulesException("Encountered a null rule element");

                default -> {}
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

    /**
     * Return true if this rule only depends on the base data graph.
     * That is, {@link #directDependencies} would return an empty collection.
     */
    public boolean isDataRule(Rule rule) {
        return level0.contains(rule);
    }


    public Collection<Edge> edges() {
        return direct.values();
    }

    public void walk(Rule rule, Consumer<Rule> action) {
        walk$(rule, action);
    }

    // -- Rule set traversal.  A depth-first walk from a rule.

    private void walk$(Rule rule, Consumer<Rule> action) {
        Set<Rule> acc = new HashSet<>();
        Deque<Rule> stack = new ArrayDeque<>();
        walk$(rule, action, acc, stack);
    }

    private void walk$(Rule rule, Consumer<Rule> action, Set<Rule> visited, Deque<Rule> pathVisited) {
        if ( visited.contains(rule) )
            return;
        visited.add(rule);
        // Action on this rule.
        action.accept(rule);
        // Traversal stack
        pathVisited.push(rule);
        walkStep(rule, action, visited, pathVisited);
        pathVisited.pop();
    }

    /** Walk from a rule using the direct connections. */
    private void walkStep(Rule rule, Consumer<Rule> action, Set<Rule> visited, Deque<Rule> pathVisited) {
        Collection<Edge> others = direct.get(rule);
        for ( Edge edge : others ) {
            walk$(edge.linkedRule, action, visited, pathVisited);
        }
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
                    ShaclRulesWriter.write(out, r, ruleSet.getPrefixMap(), true);
                }
                out.decIndent();
            }
            if ( ! direct.isEmpty() ) {
                out.println("Edges ");
                out.incIndent();
                for ( Rule r : direct.keySet() ) {
                    ShaclRulesWriter.write(out, r, ruleSet.getPrefixMap(), true);
                    out.ensureStartOfLine();
                    Collection<Edge> c = direct.get(r);
                    c.forEach(edge -> {
                        out.incIndent(EdgeOffset);
                        out.print(edge.link.symbol);
                        out.print(" ");
                        ShaclRulesWriter.write(out, edge.linkedRule, ruleSet.getPrefixMap(), true);
                        out.decIndent(EdgeOffset);
                        out.ensureStartOfLine();
                    });
                }
                out.decIndent();
            }
        } finally { out.flush(); }
    }
}
