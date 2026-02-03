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
import org.apache.jena.riot.out.NodeFmtLib;
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
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.EltNegation;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.EltTriplePattern;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph;
import org.seaborne.jena.shacl_rules.sys.RecursionChecker;
import org.seaborne.jena.shacl_rules.sys.Stratification;
import org.seaborne.jena.shacl_rules.sys.WellFormed;

/** Forward execution support */
class RulesExecLib {

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

    private static Iterator<Binding> buildEvalBody(Graph graph, Binding binding, List<RuleBodyElement> ruleElts) {
        Iterator<Binding> chain = Iter.singletonIterator(binding);
        // Extract
        for ( RuleBodyElement elt : ruleElts ) {
            Iterator<Binding> chainIn = chain;
            Iterator<Binding> chainOut = evalOneRuleElement(graph, chainIn, elt);
            chain = chainOut;
            if ( false ) {
                FmtLog.info(RulesExecLib.class, "chain: ");
                chain = Iter.log(System.out, chain);
            }
        }
        return chain;
    }

    private static Iterator<Binding> evalOneRuleElement(Graph graph, Iterator<Binding> chainIn, RuleBodyElement elt) {
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
        // Choose one!
        // In all cases, only the head template instantiation for this one
        // solution is affected.

        if ( true ) {
            // Unbound variables can't happen.
            // Matches "return null" in EltAssignment handling.
            // BIND failing should have caused the solution not to be generated.
            rule.getTripleTemplates().stream()
            .map(tripleTemplate->Substitute.substitute(tripleTemplate, solution))
            // ---- Consistency checking - no necessary
            .map(triple->{
                if ( ! triple.isConcrete() )
                    throw new RulesEvalException("Triple is not grounded: "+NodeFmtLib.displayStr(triple) );
                return triple;
            })
            // ----
            .forEach(acc::add);
            return;
        }

        // If solutions without the necessary variables are allowed by pattern matching:

        if ( false ) {
            // If skip just the triples with an unbound variables.
            // Other triple template substitutions succeed.
            rule.getTripleTemplates().stream()
                .map(tripleTemplate->Substitute.substitute(tripleTemplate, solution))
                .filter(Triple::isConcrete)
                .forEach(acc::add);
            return;
        }

        if ( false ) {
            // Skip all triples generated by a head template if there is a a template template that fails.
            // Could pre-process to know which variables are need and check the solution,
            // then add directly to the accumulator.

            // Collect into a temporary place in case we need to back out.
            List<Triple> acc1 = new ArrayList<>();
            for ( Triple tripleTemplate : rule.getTripleTemplates() ) {
                Triple t = Substitute.substitute(tripleTemplate, solution);
                if ( ! t.isConcrete() ) {
                    // Skip this whole head.
                    return;
                }
                acc1.add(t);
            }
            acc.addAll(acc1);
        }
        throw new RulesEvalException("No template instantiation step defined");
    }
}
