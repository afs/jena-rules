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
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.Rules;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph;
import org.seaborne.jena.shacl_rules.sys.RecursionChecker;
import org.seaborne.jena.shacl_rules.sys.Stratification;
import org.seaborne.jena.shacl_rules.sys.WellFormed;
import org.seaborne.jena.shacl_rules.tuples.Tuple;
import org.seaborne.jena.shacl_rules.tuples.TupleStore;
import org.seaborne.jena.shacl_rules.tuples.Tuples;

/**
 * Forward execution support.
 * This class is not API.
 */
class RulesExecLib {

    /** Perform checking and setup */
    public static void prepare(RuleSet ruleSet, RulesExecCxt rCxt) {
        WellFormed.checkWellFormed(ruleSet);
        // XXX Ought to do this once as a "prepare" step and keep in the RuleSet.
        DependencyGraph depGraph = DependencyGraph.create(ruleSet);
        RecursionChecker.checkForIllegalRecursion(depGraph);
        Stratification.create(ruleSet, depGraph);
    }

    public static RuleEval evalRule(Graph graph, TupleStore tupleStore, Rule rule, RulesExecCxt rCxt) {
        Iterator<Binding> iter = evalBody(graph, tupleStore, rule, rCxt);
        // XXX Do better - avoid creating arrays that aren't used.
        // XXX Do better - pass around accumulators?
        List<Triple> accTriple = new ArrayList<>();
        List<Tuple> accTuple = new ArrayList<>();
        Iter.forEach(iter, solution->accInstantiateHead(accTriple, accTuple, rule, solution));
        return new RuleEval(accTriple, accTuple);
    }

    private static Iterator<Binding> evalBody(Graph graph, TupleStore tupleStore, Rule rule, RulesExecCxt rCxt) {
        Binding binding = BindingFactory.binding();
        return buildEvalBody(graph, tupleStore, binding, rule.getBodyElements(), rCxt);
    }

    private static Iterator<Binding> buildEvalBody(Graph graph, TupleStore tupleStore, Binding binding, List<RuleBodyElement> ruleElts, RulesExecCxt rCxt) {
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

    private static Iterator<Binding> evalOneRuleElement(Graph graph, TupleStore tupleStore, Iterator<Binding> chainIn, RuleBodyElement elt, RulesExecCxt rCxt) {
        switch(elt) {
            case EltTriplePattern(Triple triplePattern) -> {
                return Access.accessGraph(chainIn, graph, triplePattern);
            }
            case EltTuplePattern(Tuple tuplePattern) -> {
                return AccessTuples.accessTupleStore(chainIn, tupleStore, tuplePattern, rCxt);
            }
            case EltCondition(Expr condition) -> {
                Iterator<Binding> chain2 = Iter.filter(chainIn, solution-> {
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
                        //    return row;
                        // ... or omit this solution
                        return null;
                    }
                };
                return Iter.iter(chainIn).map(mapper).removeNulls()/*.get()*/;
            }
            case EltNegation(List<RuleBodyElement> innerBody) -> {
                Iterator<Binding> chain2 = Iter.filter(chainIn, solution-> {
                    Iterator<Binding> chainInner = buildEvalBody(graph, tupleStore, solution, innerBody, rCxt);
                    boolean innerMatches = chainInner.hasNext();
                    return !innerMatches;
                });
                return chain2;
            }
        }
    }

    private static void accInstantiateHead(List<Triple> accTriples,  List<Tuple> accTuples, Rule rule, Binding solution) {
        // Unbound variables shoudln't happen.
        // Matches "return null" in EltAssignment handling.
        // BIND failing :: issue https://github.com/w3c/data-shapes/issues/753
        rule.getHeadElements().stream().forEach(headElt->{
            switch(headElt) {
                case RuleHeadElement.EltTripleTemplate(Triple tripleTemplate) -> {
                    Triple triple = Substitute.substitute(tripleTemplate, solution);
                    if ( ! triple.isConcrete() )
                        throw new RulesEvalException("Triple is not grounded: "+NodeFmtLib.displayStr(triple) );
                    accTriples.add(triple);
                }
                case RuleHeadElement.EltTupleTemplate(Tuple tupleTemplate) -> {
                    Tuple tuple = Tuples.substitute(tupleTemplate, solution);
                    if ( ! tuple.isConcrete() )
                        throw new RulesEvalException("Tuple is not grounded: "+Tuples.displayStr(tuple) );
                    accTuples.add(tuple);
                }
                case null -> throw new InternalErrorException("null head element");
            }
        });

    }

    /**
     * Create a {@link RulesExecCxt} from a {@link Context}.
     * The argument context is copied - the caller does not need to provide
     * a safe copy.
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
