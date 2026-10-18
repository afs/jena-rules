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

package org.seaborne.jena.srl.exec;

import java.util.*;
import java.util.function.Function;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.iterator.IteratorConcat;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingProject;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.modify.TemplateLib;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleBody;
import org.seaborne.jena.srl.RuleHead;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.agg.*;
import org.seaborne.jena.srl.jena.AppendGraph;
import org.seaborne.jena.srl.lang.RuleBodyElement;
import org.seaborne.jena.srl.lang.RuleBodyElement.*;
import org.seaborne.jena.srl.lang.parser.SynAggregationAssignment;
import org.seaborne.jena.srl.sys.DependencyGraph;
import org.seaborne.jena.srl.sys.RecursionChecker;
import org.seaborne.jena.srl.sys.Stratification;
import org.seaborne.jena.srl.sys.WellFormed;
import org.seaborne.jena.srl.tuples.AppendTupleStore;
import org.seaborne.jena.srl.tuples.Tuple;
import org.seaborne.jena.srl.tuples.TupleStore;
import org.seaborne.jena.srl.tuples.Tuples;

/**
 * Forward execution support. This class is not API.
 */
public/*development only*/
class RulesExecLib {

    /** Perform checking and setup */
    public static Stratification prepare(RuleSet ruleSet, RulesExecCxt rCxt) {
        //XXX [RunOnce] Do this in "rules parser ..."
        WellFormed.checkWellFormed(ruleSet);
        // XXX Ought to do this once as a "prepare" step and keep in the RuleSet.
        DependencyGraph depGraph = DependencyGraph.create(ruleSet, rCxt);
        RecursionChecker.checkForIllegalRecursion(depGraph, rCxt);
        return Stratification.create(ruleSet, depGraph, rCxt);
    }

    /** Execute a rule */
    public static RuleEval evalRule(Rule rule, Graph graph, TupleStore tupleStore, Graph inputGraph, RulesExecCxt rCxt) {
        Iterator<Binding> iter = evalBody(graph, tupleStore,  inputGraph, rule.getBody(), rCxt);
        // XXX Do better - avoid creating arrays that aren't used.
        // XXX Do better - pass around accumulators?
        RuleEval ruleEval = evalRuleHead(rule.getHead(), iter, rCxt);
        return ruleEval;
    }

    public static RuleEval evalRuleHead(RuleHead ruleHead, Iterator<Binding> iter, RulesExecCxt rCxt) {
        List<Triple> accTriple = new ArrayList<>();
        List<Tuple> accTuple = new ArrayList<>();
        Iter.forEach(iter, solution -> accInstantiateHead(accTriple, accTuple, ruleHead, solution));
        return new RuleEval(accTriple, accTuple);
    }

    public static void accumulateOneRuleHead(RuleEval rEval, Graph graph, TupleStore evalTupleStore, RulesExecCxt rCxt) {
        if ( rEval.tuples() != null && ! rEval.tuples().isEmpty() ) {
            if ( evalTupleStore == null )
                throw new RulesEvalException("No tuple store for tuples in rule output");
            evalTupleStore.addAll(rEval.tuples());
        }
        List<Triple> triples = rEval.triples();
//        for ( Triple t : triples ) {
//            if ( ! graph.contains(t) )
//                System.out.println(t);
//        }

        if ( true ) {
            triples.forEach(t-> {
                if ( !t.isConcrete() )
                    throw new RulesEvalException("Triple not concrete: "+NodeFmtLib.displayStr(t));
            });
        }

        if ( ! triples.isEmpty() )
            GraphUtil.add(graph, triples);
    }

//    /**
//     * Single evaluation pass over a list of rules, executing rule once,
//     * and in the order in the list.
//     */
//    public static AppendGraph evalRulesOnce(Graph evalGraph, TupleStore tupleStore, List<Rule> rules, RulesExecCxt rCxt) {
//        AppendGraph allGraph = AppendGraph.create(evalGraph);
//
//        for ( Rule rule : rules ) {
//            Iterator<Binding> iter = evalBody(allGraph, tupleStore, rule, rCxt);
//            List<Triple> accTriple = new ArrayList<>();
//            List<Tuple> accTuple = new ArrayList<>();
//            Iter.forEach(iter, solution -> accInstantiateHead(accTriple, accTuple, rule, solution));
//            accTriple.forEach(allGraph::add);
//            accTuple.forEach(tupleStore::add);
//        }
//        //Graph infGraph = allGraph.getAdded();
//        return allGraph;
//    }

    /**
     * Single evaluation pass over a list of rules, executing rule once,
     * and in the order in the list.
     */
    public static Evaluation _unused_evalRulesOnce(Graph evalGraph, TupleStore tupleStore, Graph inputGraph, RuleSet ruleSet) {
        RulesExecCxt rCxt = RulesExecCxt.create();
        AppendGraph allGraph = AppendGraph.create(evalGraph);
        AppendTupleStore allTuples = AppendTupleStore.create(tupleStore);

        for ( Rule rule : ruleSet.getRules() ) {
            Iterator<Binding> iter = evalBody(allGraph, tupleStore, inputGraph, rule.getBody(), rCxt);
            List<Triple> accTriple = new ArrayList<>();
            List<Tuple> accTuple = new ArrayList<>();
            Iter.forEach(iter, solution -> accInstantiateHead(accTriple, accTuple, rule.getHead(), solution));
            accTriple.forEach(allGraph::add);
            if ( ! accTuple.isEmpty() ) {
                if ( tupleStore == null )
                    throw new RulesEvalException("No tuple store for tuple outcomes");
            }
            accTuple.forEach(tupleStore::add);
        }
        return new Evaluation(evalGraph, ruleSet, allGraph.getAdded(), allGraph, allTuples.getAdded());
    }

    private static Iterator<Binding> evalBody(Graph evalGraph, TupleStore tupleStore, Graph inputGraph, RuleBody ruleBody, RulesExecCxt rCxt) {
        Binding binding = BindingFactory.binding();
        // If WHERE DATA, then pass down the inputGraph
        Graph matchGraph = ruleBody.isGrounded() ? inputGraph : evalGraph ;
        return evalBodyBinding(matchGraph, tupleStore, inputGraph, binding, ruleBody.getBodyElements(), rCxt);
    }

    private static Iterator<Binding> evalBodyBinding(Graph evalGraph, TupleStore tupleStore, Graph inputGraph, Binding binding,
                                                     List<RuleBodyElement> ruleElts, RulesExecCxt rCxt) {
        Iterator<Binding> chain = Iter.singletonIterator(binding);
        // Extract
        for ( RuleBodyElement elt : ruleElts ) {
            Iterator<Binding> chainIn = chain;
            Iterator<Binding> chainOut = evalOneRuleElement(evalGraph, tupleStore, inputGraph, chainIn, elt, rCxt);
            chain = chainOut;
            if ( false ) {
                FmtLog.info(RulesExecLib.class, "chain: ");
                chain = Iter.log(System.out, chain);
            }
        }
        return chain;
    }

    private static Iterator<Binding> evalOneRuleElement(Graph evalGraph, TupleStore tupleStore, Graph inputGraph, Iterator<Binding> chainIn,
                                                        RuleBodyElement elt, RulesExecCxt rCxt) {
        // evalGraph will be the inputGraph if this is a grounded evaluation.
        switch (elt) {
            case EltTriplePattern(Triple triplePattern) -> {
                return Access.accessGraph(chainIn, evalGraph, triplePattern, rCxt);
            }
            case EltTuplePattern(Tuple tuplePattern) -> {
                if ( tupleStore == null )
                    throw new RulesEvalException("No tuple store for tuple pattern: "+tuplePattern);

                return AccessTuples.accessTupleStore(chainIn, tupleStore, tuplePattern, rCxt);
            }
            case EltFilter(Expr condition) -> {
                Iterator<Binding> chain2 = Iter.filter(chainIn, solution -> {
                    FunctionEnv functionEnv = rCxt;
                    // ExprNode.isSatisfied converts ExprEvalException to false.
                    return condition.isSatisfied(solution, functionEnv);
                });
                return chain2;
            }
            case EltAssignment(Var var, Expr expression) -> {
                Function<Binding, Binding> mapper = row -> {
                    FunctionEnv funcEnv = rCxt;
                    try {
                        NodeValue nv = expression.eval(row, funcEnv);
                        return BindingFactory.binding(row, var, nv.asNode());
                    } catch (ExprEvalException ex) {
                        // Error in evaluation of the expression.
                        // Either continue, no binding.
                        // return row;
                        // ... or omit this solution
                        return null;
                    }
                };
                return Iter.iter(chainIn).map(mapper).removeNulls();
            }
            case EltGroup(List<Var> groupKeys, List<RuleBodyElement> aggPattern, List<SynAggregationAssignment> aggAssignments) -> {
                return aggregateAssignment(evalGraph, tupleStore, chainIn, groupKeys, aggPattern, aggAssignments, rCxt);
            }

            case EltNegation(List<RuleBodyElement> innerBody, boolean grounded) -> {
                // If NOT DATA, then match on the inputGraph baseGraph.
                Graph matchGraph = (grounded) ? inputGraph : evalGraph;
                Iterator<Binding> chain2 = Iter.filter(chainIn, solution -> {
                    Iterator<Binding> chainInner = evalBodyBinding(matchGraph, tupleStore, inputGraph, solution, innerBody, rCxt);
                    boolean innerMatches = chainInner.hasNext();
                    return !innerMatches;
                });
                return chain2;
            }
        }
    }

    public static boolean AUGMENT = true;

    private static Iterator<Binding> aggregateAssignment(Graph evalGraph, TupleStore tupleStore, Iterator<Binding> chainIn,
                                                         List<Var> groupKeys, List<RuleBodyElement> aggPattern,
                                                         List<SynAggregationAssignment> aggAssignments, RulesExecCxt rCxt) {
        GroupSplitter groupSplitter = GroupSplitters.splitter(groupKeys);
        List<AggregatorFactory> factories = new ArrayList<>();
        aggAssignments.forEach(aggAssign->{
            AggregatorFactory f =
                    switch (aggAssign.aggFunction()) {
                        case COUNT -> new Agg_Count(false, aggAssign.aggVariable(), aggAssign.expr(), rCxt);
                        case COUNT_DISTINCT -> new Agg_Count(true, aggAssign.aggVariable(), aggAssign.expr(),rCxt);
                        case MAX  -> new Agg_Max(aggAssign.aggVariable(), aggAssign.expr(), rCxt);
                        case MIN  -> new Agg_Min(aggAssign.aggVariable(), aggAssign.expr(), rCxt);
                        case SUM  -> new Agg_Sum(aggAssign.aggVariable(), aggAssign.expr(), rCxt);
                    };
            factories.add(f);
        });


        if ( AUGMENT ) {
            return aggregateAugment(groupSplitter, factories,
                                    evalGraph, tupleStore, chainIn, groupKeys, aggPattern, aggAssignments, rCxt);
        } else {
            return aggregateSummarise(groupSplitter, factories,
                                  evalGraph, tupleStore, chainIn, groupKeys, aggPattern, aggAssignments, rCxt);
        }
    }

    private static Iterator<Binding> aggregateSummarise(GroupSplitter groupSplitter, List<AggregatorFactory> factories,
                                                        // All args of aggregateAssignment
                                                        Graph evalGraph, TupleStore tupleStore, Iterator<Binding> chainIn,
                                                        List<Var> groupKeys, List<RuleBodyElement> aggPattern,
                                                        List<SynAggregationAssignment> aggAssignments, RulesExecCxt rCxt
            ) {
        // Summarise - do not pass on the full input binding, only group keys and aggregated values.
        // SRL is "augment"
        System.out.println("** Summarise");
        // Summarise
        GroupProcessor groupProc = new GroupProcessor(groupSplitter, factories);
        groupProc.startReceive();
        // Account for "row"
        chainIn.forEachRemaining(row->{
            Iterator<Binding> chainInner = evalBodyBinding(evalGraph, tupleStore, evalGraph, row, aggPattern, rCxt);
            chainInner.forEachRemaining(binding->{
                // The return from evalBodyBinding has the input binding as parent.
                // No need to merge.
//                BindingBuilder b = BindingBuilder.create(row);
//                row.forEach((var, val) ->{
//                    if ( ! b.contains(var) ) {
//                        System.out.println("ADD "+var);
//                        b.add(var, val);
//                    }
//                });
//                Binding binding2 = b.build();
                groupProc.receive(binding);
            });
        });
        groupProc.finishReceive();
        return groupProc.eval();
    }

    private static Iterator<Binding> aggregateAugment(GroupSplitter groupSplitter, List<AggregatorFactory> factories,
                                                      // All args of aggregateAssignment
                                                      Graph evalGraph, TupleStore tupleStore, Iterator<Binding> chainIn,
                                                      List<Var> groupKeys, List<RuleBodyElement> aggPattern,
                                                      List<SynAggregationAssignment> aggAssignments, RulesExecCxt rCxt
            ) {
        GroupProcessor groupProc = new GroupProcessor(groupSplitter, factories);
        Iterator<Binding> results = null;

        while(chainIn.hasNext() ) {
            Binding row = chainIn.next();
            // reset
            groupProc.startReceive();

            groupProc.nextRow(row);
            //groupProc.nextRow(BindingFactory.binding());

            // Cache same row restricted by group keys.

//          // XXX ONLY GROUP KEYS

            //BindingFactory.project()
            Binding row2 = new BindingProject(groupKeys, row);
//            // XXX Wrap in a projection row to group keys.
            // XXX ONLY GROUP KEYS
            Iterator<Binding> chainInner = evalBodyBinding(evalGraph, tupleStore, evalGraph, row2, aggPattern, rCxt);
//            var x = Iter.toList(chainInner);
//            x.forEach(System.out::println);
//            chainInner = x.iterator();
            chainInner.forEachRemaining(binding->groupProc.receive(binding));
            groupProc.finishReceive();
            Iterator<Binding> out1 = groupProc.eval();
            results = IteratorConcat.concat(results, out1);
        }
        return results;
    }

    private static void accInstantiateHead(List<Triple> accTriples, List<Tuple> accTuples, RuleHead ruleHead, Binding solution) {
        // Unbound variables shouldn't happen.
        // Make it CONSTRUCT-like (but needs conditions to enable termination)
        Iterator<Triple> iter = templateInstantiationTriples(ruleHead.getHeadTriples(), solution);
        iter.forEachRemaining(accTriples::add);
        Iterator<Tuple> iter2 = templateInstantiationTuples(ruleHead.getHeadTuples(), solution);
        iter2.forEachRemaining(accTuples::add);
    }

    private static Iterator<Triple> templateInstantiationTriples(List<Triple> headElements, Binding binding) {
        return TemplateLib.calcTriples(headElements, Iter.singletonIterator(binding));
    }

    private static Iterator<Tuple> templateInstantiationTuples(List<Tuple> headElements, Binding binding) {
        return calcTuples(headElements, Iter.singletonIterator(binding));
    }

    /** Substitute into tuple patterns */
    public static Iterator<Tuple> calcTuples(List<Tuple> tuples, Iterator<Binding> binding) {
        Function<Binding, Iterator<Tuple>> mapper = new Function<>() {
            Map<Node, Node> bNodeMap = new HashMap<>();

            @Override
            public Iterator<Tuple> apply(final Binding b) {
                // Iteration is a new mapping of bnodes.
                bNodeMap.clear();

                List<Tuple> tupleList = new ArrayList<>(tuples.size());
                for ( Tuple tuple : tuples ) {
                    Tuple q = Tuples.substituteTemplate(tuple, b, bNodeMap);
                    tupleList.add(q);
                }
                return tupleList.iterator();
            }
        };
        return Iter.flatMap(binding, mapper);
    }

    // Use RulesExecCxt.create
//    /**
//     * Create a {@link RulesExecCxt} from a {@link Context}. The argument context is
//     * copied - the caller does not need to provide a safe copy.
//     */
//    static RulesExecCxt rulesExecCxt(Context cxt) {
//        if ( cxt == null )
//            cxt = Rules.getContext();
//        // Isolated.
//        cxt = cxt.copy();
//        RulesExecCxt rCxt = RulesExecCxt.create(cxt);
//        return rCxt;
//    }
}
