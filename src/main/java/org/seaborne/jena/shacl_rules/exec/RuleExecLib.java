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
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltNegation;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltTriplePattern;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph;
import org.seaborne.jena.shacl_rules.sys.RecursionChecker;
import org.seaborne.jena.shacl_rules.sys.Stratification;
import org.seaborne.jena.shacl_rules.sys.WellFormed;

/** Forward execution support */
class RuleExecLib {

    /** Perform checking and setup */
    public static void prepare(RuleSet ruleSet) {
        WellFormed.checkWellFormed(ruleSet);
        // XXX Ought to do this once as a "prepare" step and keep in the RuleSet.
        DependencyGraph depGraph = DependencyGraph.create(ruleSet);
        RecursionChecker.checkForIllegalRecursion(depGraph);
        Stratification.create(ruleSet, depGraph);
    }

    public static Iterator<Binding> evalBody(Graph graph, Rule rule) {
        Binding binding = BindingFactory.binding();
        return buildEvalBody(graph, binding, rule);
    }

    public static Iterator<Binding> buildEvalBody(Graph graph, Binding binding, Rule rule) {
        return buildEvalBody(graph, binding, rule.getBodyElements());
    }

    private static Iterator<Binding> buildEvalBody(Graph graph, Binding binding, List<RuleElement> ruleElts) {
        Iterator<Binding> chain = Iter.singletonIterator(binding);
        // Extract
        for ( RuleElement elt : ruleElts ) {
            Iterator<Binding> chainIn = chain;
            Iterator<Binding> chainOut = evalOneRuleElement(graph, chainIn, elt);
            chain = chainOut;
            if ( false ) {
                FmtLog.info(RuleExecLib.class, "chain: ");
                chain = Iter.log(System.out, chain);
            }
        }
        return chain;
    }

    private static Iterator<Binding> evalOneRuleElement(Graph graph, Iterator<Binding> chainIn, RuleElement elt) {
        switch(elt) {
            case EltTriplePattern(Triple triplePattern) -> {
                return Access.accessGraph(chainIn, graph, triplePattern);
            }
            case EltCondition(Expr condition) -> {
                Iterator<Binding> chain2 = Iter.filter(chainIn, solution-> {
                    FunctionEnv functionEnv = new FunctionEnvBase(ARQ.getContext());
                    // ExprNode.isSatisfied converts ExprEvalException to false.
                    return condition.isSatisfied(solution, functionEnv);
                });
                return chain2;
            }
            case EltAssignment(Var var, Expr expression) -> {
                Function<Binding, Binding> mapper = row -> {
                    FunctionEnv funcEnv = new FunctionEnvBase();
                    try {
                        NodeValue nv = expression.eval(row, funcEnv);
                        return BindingFactory.binding(row, var, nv.asNode());
                    } catch (ExprEvalException ex) {
                        // Error in evaluation of the expression.
                        return row;
                    }
                };
                return Iter.map(chainIn, mapper);
            }
            case EltNegation(List<RuleElement> innerBody) -> {
                Iterator<Binding> chain2 = Iter.filter(chainIn, solution-> {
                    Iterator<Binding> chainInner = buildEvalBody(graph, solution, innerBody);
                    boolean innerMatches = chainInner.hasNext();
                    return !innerMatches;
                });
                return chain2;
            }
            //                case null -> {}
            default -> { throw new RulesEvalException(""); }
        }
    }


    public static List<Triple> evalRule(Graph graph, Rule rule) {
        Iterator<Binding> iter = evalBody(graph, rule);
        List<Triple> x = new ArrayList<>();
        Iter.forEach(iter, solution->accInstantiateHead(x, rule, solution));
        return x;
    }

    private static void accInstantiateHead(List<Triple> acc,  Rule rule, Binding solution) {
        rule.getTripleTemplates().stream()
                .map(triple->Substitute.substitute(triple, solution))
                .filter(Triple::isConcrete)
                .forEach(acc::add);
    }
}
