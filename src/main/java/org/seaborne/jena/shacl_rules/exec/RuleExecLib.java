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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltNegation;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltTriplePattern;
import org.seaborne.jena.shacl_rules.sys.RuleLib;

/** Forward execution support */
class RuleExecLib {

    public static Iterator<Binding> evalBody(Graph graph, Rule rule) {
        Binding binding = BindingFactory.binding();
        return buildEvalBody(graph, binding, rule);
    }

    public static Iterator<Binding> buildEvalBody(Graph graph, Binding binding, Rule rule) {
        Iterator<Binding> chain = Iter.singletonIterator(binding);

        for ( RuleElement elt : rule.getBodyElements() ) {
            switch(elt) {
                case EltTriplePattern(Triple triplePattern) -> {
                    chain = Access.accessGraph(chain, graph, triplePattern);
                }
                case EltCondition(Expr condition) -> {
                    Iterator<Binding> chain2 = Iter.filter(chain, solution-> {
                        FunctionEnv functionEnv = new FunctionEnvBase(ARQ.getContext());
                        // ExprNode.isSatisfied converts ExprEvalException to false.
                        return condition.isSatisfied(solution, functionEnv);
                    });
                }
                case EltAssignment(Var var, Expr expression) -> {
                    Function<Binding, Binding> mapper = row -> {
                        FunctionEnv funcEnv = new FunctionEnvBase();
                        NodeValue nv = expression.eval(row, funcEnv);
                        return BindingFactory.binding(row, var, nv.asNode());
                    };
                    return Iter.map(chain, mapper);
                }

                case EltNegation(List<RuleElement> innerBody) -> {
                    // XXX Temp use SPARQL
                    ElementGroup innerGroup = RuleLib.ruleEltsToElementGroup(innerBody);
                    Expr expression = new E_NotExists(innerGroup);
                    DatasetGraph dsg = DatasetGraphFactory.wrap(graph);
                    Iterator<Binding> chain2 = Iter.filter(chain, solution-> {
                        // Include the graph.
                        FunctionEnv functionEnv = new FunctionEnvBase(ARQ.getContext(), graph, dsg);
                        return expression.isSatisfied(solution, functionEnv);
                    });

                    chain = chain2;
                }
//                case null -> {}
//                default -> {}
            }
            if ( false ) {
                FmtLog.info(RuleExecLib.class, "chain: ");
                chain = Iter.log(System.out, chain);
            }
        }
        return chain;
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
                .forEach(acc::add);
    }
}
