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
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.graph.GraphFactory;
import org.seaborne.jena.shacl_rules.EngineType;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesEngine;
import org.seaborne.jena.shacl_rules.cmds.Access;
import org.seaborne.jena.shacl_rules.jena.AppendGraph;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltTriplePattern;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph;
import org.seaborne.jena.shacl_rules.sys.RuleDependencies;

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
    public Graph baseGraph() {
        return baseGraph;
    }

    @Override
    public Graph materializedGraph() {
        Evaluation e = solveTop(queryTripleAll);
        return e.outputGraph;
    }

    @Override
    public RuleSet ruleSet() {
        return ruleSet;
    }

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
        Rule query = Rule.create(List.of(queryTriple), List.of());

        // Collects all inferred triples from rules touched during the evaluation.
        // Filter inferred triples to find the ones we want.
        Evaluation e = solveTop(queryTriple);
        Stream<Triple> x = e.outputGraph.stream(queryTriple.getSubject(), queryTriple.getPredicate(), queryTriple.getObject());
        if ( TRACE ) {
            LOG.printf(">> query(%s)\n", str(queryTriple, ruleSet.getPrefixMap()));
            x = trace(LOG, x);
        }
        return x;
    }

    private static Var varS = Var.alloc("s");
    private static Var varP = Var.alloc("p");
    private static Var varO = Var.alloc("o");
    private static Triple queryTripleAll = Triple.create(varS, varP, varO);

    @Override
    public Graph infer() {
        Evaluation e = solveTop(queryTripleAll);

        Graph graph = e.inferredTriples;
        graph.getPrefixMapping().setNsPrefixes(Prefixes.adapt(ruleSet.getPrefixMap()));
        graph.getPrefixMapping().setNsPrefixes(baseGraph.getPrefixMapping());
        return graph;
    }

    /**
     * Solver. Non-recursive rules.
     * Top of evaluation.
     */
    Evaluation solveTop(Triple queryTriple) {
        if ( TRACE ) {
            LOG.printf("solve(%s)\n", str(queryTriple, ruleSet.getPrefixMap()));
            LOG.incIndent();
        }

        AppendGraph workingGraph = AppendGraph.create(baseGraph);
        if ( ruleSet.hasData() )
            ruleSet.getDataTriples().forEach(workingGraph::add);

        PrefixMap pmap = ruleSet.getPrefixMap();


        // Detect cycles.
        //Set<Triple> visited = new HashSet<>();

        // Look in workingGraph
        Stream<Triple> inf = workingGraph.find(queryTriple).toList().stream();
        if ( TRACE ) {
            LOG.println("workingGraph");
            inf = trace(LOG, inf);
        }
        // Find all rules that generate output (and other triples) for queryTriple.

        List<Rule> dependsOn = dependsOn(queryTriple);

        // Is working graph used at all?
        // XXX Is there two loops?
        // solveRule has the same pattern -  dependsOn->solveRule

        for ( Rule rule : dependsOn ) {
            Set<Rule> visited = new HashSet<>();
            Stream<Triple> x = solveRule(rule, workingGraph, visited);
            // No variables in triples :

            if ( x == null )
                continue;

//            // CHECK
//            if ( true ) {
//                List<Triple> triples = x.toList();
//                List<Triple> unexpected = triples.stream().filter(triple->!triple.isConcrete()).toList();
//                if ( ! unexpected.isEmpty() ) {
//                    System.err.println("Unexpected: "+unexpected);
//                }
//                x = triples.stream();
//            }
//            // /CHECK

            // New graph?
            GraphUtil.add(workingGraph, x.iterator());
        }

        LOG.decIndent();
//        List<Triple> solution = new ArrayList<>();
//
//        workingGraph.find(queryTriple).forEach(triple->{
//            solution.add(triple);
//        });

        Graph inferred = workingGraph.getAdded();
        Graph output = GraphFactory.createGraphMem();

        GraphUtil.addInto(output, baseGraph);
        GraphUtil.addInto(output, inferred);
        if ( ruleSet.hasData() )
            GraphUtil.addInto(output, ruleSet.getData());

        Evaluation e = new Evaluation(workingGraph.get(), ruleSet, inferred, output, -1);
        return e;
    }

    public record Evaluation(Graph baseGraph, RuleSet ruleSet, Graph inferredTriples, Graph outputGraph, int rounds) {}

    // XXX DependencyGraph
    private List<Rule> dependsOn(Triple queryTriple)  {
        List<Rule> dependsOn = new ArrayList<>();
        for ( Rule rule : ruleSet.getRules() ) {
            // XXX Check this!!!
            if ( RuleDependencies.dependsOn(queryTriple, rule) ) {
                dependsOn.add(rule);
            }
        }
        return dependsOn;
    }

    private Stream<Triple> solveRule(Rule rule, AppendGraph workingGraph, Set<Rule> visited) {
       //  trigger by solve(?,?,?)
        if ( visited.contains(rule) )
            throw new RuleEvalException("Recursion (or DAG): "+rule);
        visited.add(rule);

        if ( TRACE ) {
            LOG.printf("solveRule(%s)\n", rule.toString());
        }

        // Start
        Binding binding = BindingFactory.binding();
        Iterator<Binding> chain = Iter.singletonIterator(binding);

        if ( TRACE )
            LOG.incIndent();

        for ( RuleElement elt : rule.getBodyElements() ) {
            switch(elt) {
                case EltTriplePattern(Triple triplePattern) -> {
                    List<Rule> subRules = dependsOn(triplePattern);
                    for ( Rule subRule : subRules ) {
                        Stream<Triple> sub = solveRule(subRule, workingGraph, visited);
                        sub.forEach(workingGraph::add);
                    }
                    chain = Access.accessGraph(chain, workingGraph, triplePattern);
                }
                case EltCondition(Expr condition) -> {
                    chain = Iter.filter(chain, solution-> {
                        FunctionEnv functionEnv = new FunctionEnvBase(ARQ.getContext());
                        // ExprNode.isSatisfied converts exceptions to ExprEvalException
                        return condition.isSatisfied(solution, functionEnv);
                    });
                }
                case EltAssignment(Var var, Expr expression) -> {
                    throw new NotImplemented();
                }
//                case null -> {}
//                default -> {}}
            }

            if ( TRACE ) {
                LOG.print("chain: ");
                chain = trace(LOG, chain);
            }
        }

        //
        // Change to skip "instantiate" - needs a renamer.
        if ( TRACE )
            LOG.decIndent();

        // Instantiate the head and store in working graph. (new graph? XXX)
        List<Triple> head = rule.getTripleTemplates();
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

    private static class RuleEvalException extends RuntimeException {
        RuleEvalException(String msg) { super(msg); }
        RuleEvalException(String msg, Throwable th) { super(msg, th); }
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

    private static String str(Rule rule, PrefixMap prefixMap) {
        return str(rule.getTripleTemplates(), prefixMap) + " :- " +         str(rule.getDependentTriples(), prefixMap);
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
        for ( Triple headTriple : r.getTripleTemplates() ) {
            if ( RuleDependencies.dependsOn(headTriple, queryTriple) ) {
                return true;
            }
        }
        return false;
    }
//
//    private boolean provides(Triple queryTriple, Triple headTriple) {
//        return false;
//    }
//
}
