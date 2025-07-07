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

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.cmds.Access;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltTriplePattern;

/** Forward execution support */
public class RuleExec {

    public static Iterator<Binding> evalBody(Graph graph, Rule rule) {
        Binding binding = BindingFactory.binding();
        return buildEvalBody(graph, binding, rule);
    }

    public static Iterator<Binding> buildEvalBody(Graph graph, Binding binding, Rule rule) {
        Iterator<Binding> chain = Iter.singletonIterator(binding);

        for ( RuleElement elt : rule.getBody().getBodyElements() ) {
            switch(elt) {
                case EltTriplePattern(Triple triplePattern) -> {
                    chain = Access.accessGraph(chain, graph, triplePattern);
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
            if ( false ) {
                FmtLog.info(RuleExec.class, "chain: ");
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
        rule.getHead().getTriples().stream()
                .map(triple->Substitute.substitute(triple, solution))
                .forEach(acc::add);
    }
}
