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

package org.seaborne.jena.shacl_rules.exec;

import java.util.*;
import java.util.stream.Stream;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.system.buffering.BufferingGraph;
import org.seaborne.jena.shacl_rules.EngineType;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesEngine;
import org.seaborne.jena.shacl_rules.cmds.Access;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph;

/*
 * A simple backwards chaining rule engine.
 * It does not support recursion.
 */
public class RulesEngineBkdNonRecursive implements RulesEngine {

    private boolean TRACE = false;
    @Override
    public RulesEngineBkdNonRecursive setTrace(boolean traceSetting) {
        TRACE = traceSetting;
        return this;
    }

    public static IndentedWriter LOG = IndentedWriter.stdout.clone().setFlushOnNewline(true).setLinePrefix("R: ");

    public static RulesEngineBkdNonRecursive build(Graph graph, RuleSet ruleSet) {
        return new RulesEngineBkdNonRecursive(graph, ruleSet);
    }

    private final RuleSet ruleSet;
    private final DependencyGraph dependencyGraph;
    private final Graph baseGraph;

    private RulesEngineBkdNonRecursive(Graph baseGraph, RuleSet ruleSet) {
        this.baseGraph = baseGraph;
        this.ruleSet = ruleSet;
        this.dependencyGraph = DependencyGraph.create(ruleSet);
    }

    @Override
    public EngineType engineType() {
        return EngineType.BKD_NON_RECURSIVE_SLD;
    }

    @Override
    public Graph graph() {
        Graph graph = GraphFactory.createDefaultGraph();
        GraphUtil.addInto(graph, baseGraph);
        Graph g2 = infer();
        GraphUtil.addInto(graph, g2);
        return graph;
    }

    @Override
    public RuleSet ruleSet() {
        return ruleSet;
    }

    //static ElementRule emptyRule0 = ShaclRulesParser.parseString("RULE {} WHERE {}").getRules().getFirst();

    //static ElementRule emptyRule1 = Rule.create(List.of(), new ElementRule(new BasicPattern(), new ElementGroup()));

    @Override
    public Stream<Triple> solve(Node s, Node p, Node o) {
        // Setup.
        Triple queryTriple = Triple.create(s, p, o);
        return solve(queryTriple);
    }

    @Override
    public Stream<Triple> solve(Triple queryTriple) {
        if ( TRACE )
            LOG.printf("<< query(%s)\n", str(queryTriple, ruleSet.getPrefixMap()));
        // Do better! Query subClass of Rule.
        Rule query = Rule.create(List.of(queryTriple), new ElementGroup());

        // Match data.
        BufferingGraph workingGraph = BufferingGraph.create(baseGraph);
        //ruleSet.getDataTriples().forEach(workingGraph::add);

        Stream<Triple> x = solve(queryTriple, workingGraph);
        if ( TRACE ) {
            LOG.printf(">> query(%s)\n", str(queryTriple, ruleSet.getPrefixMap()));
            x = trace(LOG, x);
        }

        // Does not fill working graph?
        //return workingGraph.getAdded().stream();
        return x;
    }

    @Override
    public Graph infer() {
        Stream<Triple> all = solve(null, null, null);
        Graph graph = GraphFactory.createDefaultGraph();
        all.forEach(graph::add);
        return graph;
    }

    /**
     * Solver. Non-recursive rules.
     * Nested evaluation.
     */
    private Stream<Triple> solve(Triple queryTriple, BufferingGraph workingGraph) {
        if ( TRACE ) {
            LOG.printf("solve(%s)\n", str(queryTriple, ruleSet.getPrefixMap()));
            LOG.incIndent();
        }
        PrefixMap pmap = ruleSet.getPrefixMap();

        // Detect cycles.
        Set<Rule> visited = new HashSet<>();
        //Set<Triple> visited = new HashSet<>();

        // Look in workingGraph
        Stream<Triple> inf = workingGraph.find(queryTriple).toList().stream();
        if ( TRACE ) {
            LOG.println("workingGraph");
            inf = trace(LOG, inf);
        }
        // Find all rules that generate output (and other triples) for queryTriple.

        List<Rule> dependsOn = dependsOn(queryTriple);

        for ( Rule rule : dependsOn ) {
            Stream<Triple> x = solveRule(rule, workingGraph, visited);
            // No variables in triples :

            if ( x == null )
                continue;

            // CHECK
            if ( true ) {
                List<Triple> triples = x.toList();
                List<Triple> unexpected = triples.stream().filter(triple->!triple.isConcrete()).toList();
                if ( ! unexpected.isEmpty() ) {
                    System.err.println("Unexpected: "+unexpected);
                }
                x = triples.stream();
            }
            // /CHECK

            // New graph?
            GraphUtil.add(workingGraph, x.iterator());
        }

        List<Triple> solution = new ArrayList<>();
        workingGraph.find(queryTriple).forEach(triple->{
            solution.add(triple);
        });

        Stream<Triple> results = solution.stream();
        LOG.decIndent();
        return (results == null) ? Stream.empty() : results;
    }

    // XXX DependencyGraph
    private List<Rule> dependsOn(Triple queryTriple)  {
        List<Rule> dependsOn = new ArrayList<>();
        for ( Rule rule : ruleSet.getRules() ) {
            // XXX Check this!!!
            if ( RuleOps.dependsOn(queryTriple, rule) ) {
                dependsOn.add(rule);
            }
        }
        return dependsOn;
    }

    /** Print immediate, noting empty streams */
    private static <X> Stream<X> trace(IndentedWriter out, Stream<X> stream) {
        out.incIndent();
        try {
            if ( stream == null ) {
                out.println("[null]");
                return stream;
            }
            List<X> elts = stream.toList();
            trace(out, elts);
            return elts.stream();
        } finally { out.decIndent(); }
    }

    /** Print immediate, noting empty iterators */
    private static <X> Iterator<X> trace(IndentedWriter out, Iterator<X> iter) {
        out.incIndent();
        try {
            if ( iter == null ) {
                out.println("[null]");
                return iter;
            }
            List<X> elts = Iter.toList(iter);
            trace(out, elts);
            return elts.iterator();
        } finally { out.decIndent(); }
    }

    /** Print */
    private static <X> void trace(IndentedWriter out, List<X> elts) {
        out.incIndent();
        try {
            if ( elts == null ) {
                out.println("[null]");
                return;
            }
            if ( elts.isEmpty() )
                out.println("[empty]");
            else {
                StringJoiner sj = new StringJoiner(" | ", "[ ", " ]");
                elts.forEach(b->sj.add(b.toString()));
                out.println(sj.toString());
            }
        } finally { out.decIndent(); }
    }


    private static class RuleEvalException extends RuntimeException {
        RuleEvalException(String msg) { super(msg); }
        RuleEvalException(String msg, Throwable th) { super(msg, th); }
    }

    private Stream<Triple> solveRule(Rule rule, BufferingGraph workingGraph, Set<Rule> visited) {
        if ( visited.contains(rule) )
            throw new RuleEvalException("Recursion (or DAG): "+rule);

        if ( TRACE ) {
            LOG.printf("solveRule(%s)\n", rule.toString());
        }

        List<Triple> body = rule.getBody().getTriples();
        Stream<Triple> results = null;

        // Start
        Binding binding = BindingFactory.binding();
        Iterator<Binding> chain = Iter.singletonIterator(binding);

        if ( TRACE )
            LOG.incIndent();
        for ( Triple pattern : body ) {
            List<Rule> subRules = dependsOn(pattern);
            if ( TRACE ) {
                LOG.printf("DependsOn: %s\n", subRules.toString());
            }

            // solve these and then
            // Binding + rename to avoid triples.
            // Later.

            for ( Rule subRule : subRules ) {
                // Stream<Triple> solveRule(Rule rule, BufferingGraph workingGraph, Set<Rule> visited) {
                Stream<Triple> sub = solveRule(subRule, workingGraph, visited);
                sub.forEach(workingGraph::add);
            }

            chain = Access.accessGraph(chain, workingGraph, pattern);

            if ( TRACE ) {
                LOG.print("chain: ");
                chain = trace(LOG, chain);
            }
        }

        // Change to skip "instantiate" - needs a renamer.
        if ( TRACE )
            LOG.decIndent();

        // Instantiate the head and store in working graph. (new graph? XXX)
        List<Triple> head = rule.getHead().getTriples();
        BasicPattern bgp = BasicPattern.wrap(head);
        List<Triple> result = new ArrayList<>();
        while(chain.hasNext()) {
            Binding row = chain.next();
            BasicPattern outcome = Substitute.substitute(bgp, row);
            result.addAll(outcome.getList());
        }

        if ( TRACE ) {
            trace(LOG, result);
        }

        return result.stream();
    }
//
//    private Stream<Triple> solveRule0(Rule rule, BufferingGraph workingGraph, Set<Rule> visited) {
//        if ( visited.contains(rule) )
//            throw new RuleEvalException("Recursion (or DAG): "+rule);
//
//        if ( TRACE ) {
//            LOG.printf("solveRule(%s)\n", rule.toString());
//        }
//
//        List<Triple> body = rule.getBody().getTriples();
//        Stream<Triple> results = null;
//
//        // Start
//        Binding binding = BindingFactory.binding();
//        Iterator<Binding> chain = Iter.singletonIterator(binding);
//        ExecutionContext execCxt = ExecutionContext.createForGraph(workingGraph);
//
//        if ( TRACE )
//            LOG.incIndent();
//        for ( Triple pattern : body ) {
//            // Step 1
//            // Look for rules that are needed. Evaluate.
//            // This is a non-recusive engine so that is moving towards the base graph.
//            // Depth-first
//            // Evaluation will fill the workingGraph graph.
//
//            //for all sub rules.
//
//
//            // **** FIND SUB RULES
//            List<Rule> subRules = dependsOn(pattern);
//            if ( TRACE ) {
//                LOG.printf("DependsOn: %s\n", subRules.toString());
//            }
//
//            // solve these and then
//            // Binding + rename to avoid triples.
//            // Later.
//
//            for ( Rule subRule : subRules ) {
//                // Stream<Triple> solveRule(Rule rule, BufferingGraph workingGraph, Set<Rule> visited) {
//                Stream<Triple> sub = solveRule(subRule, workingGraph, visited);
//                sub.forEach(workingGraph::add);
//            }
//
//
//            // NEED TO consider "find" on the workingGraph
//
//            // XXX REWRITE for RULES
//            // FlatMap it!
//
//            chain = StageMatchTriple.accessTriple(chain, workingGraph, pattern, null/*filter*/, execCxt);
//
//            if ( TRACE ) {
//                LOG.print("chain: ");
//                chain = trace(LOG, chain);
//            }
//        }
//        if ( TRACE )
//            LOG.decIndent();
//
//        // Instantiate the head and store in working graph. (new graph? XXX)
//        List<Triple> head = rule.getHead().getTriples();
//        BasicPattern bgp = BasicPattern.wrap(head);
//        List<Triple> result = new ArrayList<>();
//        while(chain.hasNext()) {
//            Binding row = chain.next();
//            BasicPattern outcome = Substitute.substitute(bgp, row);
//            result.addAll(outcome.getList());
//        }
//
//        if ( TRACE ) {
//            trace(LOG, result);
//        }
//
//        return result.stream();
//    }
//
//    private Stream<Triple> eval(Iterator<Binding> input, Triple triplePattern, Graph workingGraph) {
//        PatternMatchData.execute(graph, pattern, input, filter, execCxt);
//
//    }

    private static String str(Rule rule, PrefixMap prefixMap) {
        return str(rule.getHead().getTriples(), prefixMap) + " :- " +         str(rule.getBody().getTriples(), prefixMap);
    }

    private static String str(List<Triple> triples, PrefixMap prefixMap) {
        StringJoiner sj = new StringJoiner(", ");
        triples.forEach(t->sj.add(str(t, prefixMap)));
        return sj.toString();
    }

    private static String str(Triple triple, PrefixMap prefixMap) {
        return str(triple.getSubject(), prefixMap)+" "+str(triple.getPredicate(), prefixMap)+" "+str(triple.getObject(), prefixMap);
    }

    private static String str(Node n, PrefixMap pmap) {
        if ( n.isURI() ) {
            String x = pmap.abbreviate(n.getURI());
            if ( x != null )
                return x;
        }
        return NodeFmtLib.strTTL(n);
    }

    private boolean mayGenerate(Triple queryTriple, Rule r) {
        for ( Triple headTriple : r.getHead().getTriples() ) {
            if ( RuleOps.dependsOn(headTriple, queryTriple) ) {
                return true;
            }
        }
        return false;
    }

    private boolean provides(Triple queryTriple, Triple headTriple) {
        return false;
    }

}
