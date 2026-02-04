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

package org.seaborne.jena.shacl_rules;

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.main.solver.SolverRX3;
import org.seaborne.jena.shacl_rules.exec.EngineType;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.exec.RulesEngineRegistry;

/**
 * A {@code RulesEngine} is an execution engine for a given {@link RuleSet} and given
 * {@link Graph}.
 * <p>
 * Unless otherwise noted, a {@code RulesEngine} can execute multiple requests and
 * supports concurrent use.
 */
public interface RulesEngine {

    /**
     * Create a system default rules engine for the graph data and the rule set.
     */
    public static RulesEngine create(Graph graph, RuleSet ruleSet) {
        return create(EngineType.SIMPLE, graph, ruleSet);
    }

    /**
     * Create a rules engine for the graph data and the rule set.
     */
    public static RulesEngine create(EngineType engineType, Graph graph, RuleSet ruleSet) {
        return RulesEngineRegistry.get().create(engineType, graph, ruleSet, Rules.getContext());
    }

    public EvalAlgorithm engineType();

    /** Graph over which the rules engine executes. */
    public Graph baseGraph();

    /** The {@link RuleSet} of this RuleEngine instance. */
    public RuleSet ruleSet();

    /** All triples - from rules (including DATA) and from the base graph. */
    public Graph materializedGraph();

    /**
     * Execute, and return a graph of inferred triples that do not occur
     * in the base graph. The base graph is not modified.
     */
    public Graph infer();

    /**
     * Query (in the sense of datalog).
     * <p>Return all the triples that match the s/p/o
     * that occur in the data graph or can be inferred by the rules engine.
     */
    public Stream<Triple> solve(Node s, Node p, Node o);

    /**
     * Query (in the sense of datalog)
     * <p>Return all the triples that match the s/p/o
     * that occur in the data graph or can be inferred by the rules engine.
     */
    public default Stream<Triple> solve(Triple triple) {
        return solve(triple.getSubject(), triple.getPredicate(), triple.getObject());
    }

    /**
     * Calculate all the bindings that satisfy a triple pattern (i.e. with variables)
     * that occur in the data graph or can be inferred by the rules engine.
     */
    public default Stream<Binding> match(Triple triplePattern) {
        // Revisit - invert with "solve" and change RuleEngines
        Binding root = BindingFactory.noParent;
        Stream<Triple> stream = solve(triplePattern);
        return stream.map(dataTriple -> SolverRX3.matchTriple(root, dataTriple, triplePattern));
    }

    public RuleSetEvaluation eval();

//    /**
//     * Execute the rule set and enrich the base graph.
//     * <em>The base graph is modified.</em>
//     */
//    public default void execute() {
//        RuleSetEvaluation e = eval();
//        Graph inferredGraph = e.inferredTriples();
//        baseGraph()
//            .getTransactionHandler()
//            .executeAlways( ()-> GraphUtil.addInto( baseGraph(), inferredGraph) );
//    }

    /**
     * For development: enable trace mode for the engine.
     * Returns "this".
     *
     */
    public RulesEngine setTrace(boolean traceSetting);
}
