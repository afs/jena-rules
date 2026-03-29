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

package org.seaborne.jena.shacl_rules.exec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.modify.TemplateLib;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.Rules;
import org.seaborne.jena.shacl_rules.jena.AppendGraph;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph;
import org.seaborne.jena.shacl_rules.sys.RecursionChecker;
import org.seaborne.jena.shacl_rules.sys.Stratification;
import org.seaborne.jena.shacl_rules.sys.WellFormed;
import org.seaborne.jena.shacl_rules.tuples.AppendTupleStore;
import org.seaborne.jena.shacl_rules.tuples.Tuple;
import org.seaborne.jena.shacl_rules.tuples.TupleStore;
import org.seaborne.jena.shacl_rules.tuples.Tuples;

/**
 * Forward execution support. This class is not API.
 */
public class RulesExecLib {

    // XXX Fixme!!!
    public static boolean rewriteBlankNodes = false;

    /** Perform checking and setup */
    public static void prepare(RuleSet ruleSet, RulesExecCxt rCxt) {
        WellFormed.checkWellFormed(ruleSet);
        // XXX Ought to do this once as a "prepare" step and keep in the RuleSet.
        DependencyGraph depGraph = DependencyGraph.create(ruleSet);
        RecursionChecker.checkForIllegalRecursion(depGraph);
        Stratification.create(ruleSet, depGraph);
    }

    public static RuleEval evalRule(Rule rule, Graph graph, TupleStore tupleStore, RulesExecCxt rCxt) {
        Iterator<Binding> iter = evalBody(graph, tupleStore, rule, rCxt);
        // XXX Do better - avoid creating arrays that aren't used.
        // XXX Do better - pass around accumulators?
        List<Triple> accTriple = new ArrayList<>();
        List<Tuple> accTuple = new ArrayList<>();
        Iter.forEach(iter, solution -> accInstantiateHead(accTriple, accTuple, rule, solution));
        return new RuleEval(accTriple, accTuple);
    }

//    /**
//     * Single evaluation pass over a list of rules, executing rule once,
//     * and in the order in the list.
//     */
//    public static AppendGraph evalRulesOnce(Graph graph, TupleStore tupleStore, List<Rule> rules, RulesExecCxt rCxt) {
//        AppendGraph allGraph = AppendGraph.create(graph);
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
    public static Evaluation evalRulesOnce(Graph baseGraph, TupleStore tupleStore, RuleSet ruleSet) {
        RulesExecCxt rCxt = RulesExecCxt.create();
        AppendGraph allGraph = AppendGraph.create(baseGraph);
        AppendTupleStore allTuples = AppendTupleStore.create(tupleStore);

        for ( Rule rule : ruleSet.getRules() ) {
            Iterator<Binding> iter = evalBody(allGraph, tupleStore, rule, rCxt);
            List<Triple> accTriple = new ArrayList<>();
            List<Tuple> accTuple = new ArrayList<>();
            Iter.forEach(iter, solution -> accInstantiateHead(accTriple, accTuple, rule, solution));
            accTriple.forEach(allGraph::add);
            accTuple.forEach(tupleStore::add);
            // Print progress?
        }
        return new Evaluation(baseGraph, ruleSet, allGraph.getAdded(), allGraph, allTuples.getAdded());
    }


    private static Iterator<Binding> evalBody(Graph graph, TupleStore tupleStore, Rule rule, RulesExecCxt rCxt) {
        Binding binding = BindingFactory.binding();
        return buildEvalBody(graph, tupleStore, binding, rule.getBodyElements(), rCxt);
    }

    private static Iterator<Binding> buildEvalBody(Graph graph, TupleStore tupleStore, Binding binding, List<RuleBodyElement> ruleElts,
                                                   RulesExecCxt rCxt) {
        Iterator<Binding> chain = Iter.singletonIterator(binding);
        // Extract
        for ( RuleBodyElement elt : ruleElts ) {
            Iterator<Binding> chainIn = chain;
            Iterator<Binding> chainOut = evalOneRuleElement(graph, tupleStore, chainIn, elt, rCxt);
            chain = chainOut;
            if ( false ) {
                FmtLog.info(RulesExecLib.class, "chain: ");
                chain = Iter.log(System.out, chain);
            }
        }
        return chain;
    }

    private static Iterator<Binding> evalOneRuleElement(Graph graph, TupleStore tupleStore, Iterator<Binding> chainIn, RuleBodyElement elt,
                                                        RulesExecCxt rCxt) {
        switch (elt) {
            case EltTriplePattern(Triple triplePattern) -> {
                return Access.accessGraph(chainIn, graph, triplePattern);
            }
            case EltTuplePattern(Tuple tuplePattern) -> {
                return AccessTuples.accessTupleStore(chainIn, tupleStore, tuplePattern, rCxt);
            }
            case EltCondition(Expr condition) -> {
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
                return Iter.iter(chainIn).map(mapper).removeNulls()/* .get() */;
            }
            case EltNegation(List<RuleBodyElement> innerBody) -> {
                Iterator<Binding> chain2 = Iter.filter(chainIn, solution -> {
                    Iterator<Binding> chainInner = buildEvalBody(graph, tupleStore, solution, innerBody, rCxt);
                    boolean innerMatches = chainInner.hasNext();
                    return !innerMatches;
                });
                return chain2;
            }
        }
    }

    private static void accInstantiateHead(List<Triple> accTriples, List<Tuple> accTuples, Rule rule, Binding solution) {
        // Unbound variables shouldn't happen.
        // BIND failing needs addressing :: issue
        // https://github.com/w3c/data-shapes/issues/753

        // Should rewrite for blank nodes but that causes non-termination.

        // XXX Update for proper blank node handling in the rule head.
        if ( !rewriteBlankNodes ) {
            rule.getHeadElements().stream().forEach(headElt -> {
                switch (headElt) {
                    case RuleHeadElement.EltTripleTemplate(Triple tripleTemplate) -> {
                        Triple triple = Substitute.substitute(tripleTemplate, solution);
                        if ( !triple.isConcrete() )
                            throw new RulesEvalException("Triple is not grounded: " + NodeFmtLib.displayStr(triple));
                        accTriples.add(triple);
                    }
                    case RuleHeadElement.EltTupleTemplate(Tuple tupleTemplate) -> {
                        Tuple tuple = Tuples.substitute(tupleTemplate, solution);
                        if ( !tuple.isConcrete() )
                            throw new RulesEvalException("Tuple is not grounded: " + Tuples.displayStr(tuple));
                        accTuples.add(tuple);
                    }
                    case null -> throw new InternalErrorException("null head element");
                }
            });
        } else {
            // Make it CONSTRUCT-like (but needs conditions to enable termination)
            Iterator<Triple> iter = templateInstantiation(rule.getHeadTriples(), solution);
            iter.forEachRemaining(accTriples::add);
        }
    }

    // No tuples.
    private static Iterator<Triple> templateInstantiation(List<Triple> headElements, Binding binding) {
        return TemplateLib.calcTriples(headElements, Iter.singletonIterator(binding));
    }

    // From TemplateLib.calcTriples

// private static List<Triple> rewriteForBlankNodes(List<RuleHeadElement>
// headElements) {
// /** Substitute into triple patterns */
// public static Iterator<Triple> calcTriples(final List<Triple> triples,
// Iterator<Binding> bindings) {
// Function<Binding, Iterator<Triple>> mapper = new Function<>() {
// Map<Node, Node> bNodeMap = new HashMap<>();
//
// @Override
// public Iterator<Triple> apply(final Binding b) {
// // Iteration is a new mapping of bnodes.
// bNodeMap.clear();
//
// List<Triple> tripleList = new ArrayList<>(triples.size());
// for ( Triple triple : triples ) {
// Triple q = subst(triple, b, bNodeMap);
// if ( !q.isConcrete() || ! NodeUtils.isValidAsRDF(q.getSubject(), q.getPredicate(),
// q.getObject()) ) {
// // Log.warn(TemplateLib.class, "Unbound quad:
// // "+FmtUtils.stringForQuad(quad)) ;
// continue;
// }
// tripleList.add(q);
// }
// return tripleList.iterator();
// }
// };
// return Iter.flatMap(bindings, mapper);
// }
// }
//
// /** Substitute into a triple, with rewriting of bNodes */
// public static Triple subst(Triple triple, Binding b, Map<Node, Node> bNodeMap) {
// Node s = triple.getSubject();
// Node p = triple.getPredicate();
// Node o = triple.getObject();
//
// Node s1 = s;
// Node p1 = p;
// Node o1 = o;
//
// if ( s1.isBlank() || Var.isBlankNodeVar(s1) )
// s1 = newBlank(s1, bNodeMap);
//
// if ( p1.isBlank() || Var.isBlankNodeVar(p1) )
// p1 = newBlank(p1, bNodeMap);
//
// if ( o1.isBlank() || Var.isBlankNodeVar(o1) )
// o1 = newBlank(o1, bNodeMap);
//
// Triple t = triple;
// if ( s1 != s || p1 != p || o1 != o )
// t = Triple.create(s1, p1, o1);
// Triple t2 = Substitute.substitute(t, b);
// return t2;
// }
//
// /** generate a blank node consistently */
// private static Node newBlank(Node n, Map<Node, Node> bNodeMap) {
// if ( !bNodeMap.containsKey(n) )
// bNodeMap.put(n, NodeFactory.createBlankNode());
// return bNodeMap.get(n);
// }

    /**
     * Create a {@link RulesExecCxt} from a {@link Context}. The argument context is
     * copied - the caller does not need to provide a safe copy.
     */
    public static RulesExecCxt rulesExecCxt(Context cxt) {
        if ( cxt == null )
            cxt = Rules.getContext();
        // Isolated.
        cxt = cxt.copy();
        RulesExecCxt rCxt = RulesExecCxt.create(cxt);
        return rCxt;
    }
}
