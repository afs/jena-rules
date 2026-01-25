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

import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.graph.GraphFactory;
import org.seaborne.jena.shacl_rules.*;
import org.seaborne.jena.shacl_rules.jena.AppendGraph;

/**
 * A simple rules engine that can be easily understood.
 * <b>Caution</b>This engine does not handle stratified negation.
 * <p>
 * This is used for testing by running an engine under test and this
 * engine then comparing the results.
 */
public class RulesEngineFwdSimple0 implements RulesEngine {

    private boolean TRACE = false;
    @Override
    public RulesEngineFwdSimple0 setTrace(boolean traceSetting) {
        TRACE = traceSetting;
        return this;
    }

    public static RulesEngineFwdSimple0 build(Graph graph, RuleSet ruleSet) {
        RuleExecLib.prepare(ruleSet);
        return new RulesEngineFwdSimple0(graph, ruleSet);
    }

    private final RuleSet ruleSet;
    private final Graph baseGraph;

    private RulesEngineFwdSimple0(Graph baseGraph, RuleSet ruleSet) {
        this.baseGraph = baseGraph;
        this.ruleSet = ruleSet;
    }

    @Override
    public EngineType engineType() {
        return EngineType.FWD_NAIVE;
    }

    @Override
    public Graph baseGraph() {
        return baseGraph;
    }

    @Override
    public Graph materializedGraph() {
        RuleSetEvaluation e = eval();
        return e.outputGraph();
    }
    @Override
    public RuleSet ruleSet() {
        return ruleSet;
    }

    /**
     * This function calculates by all triples, then matches the pattern given.
     */

    @Override
    public Stream<Triple> solve(Node s, Node p, Node o) {
        // Rather than cache, wrap in a "materialize and match" RulesEngine.
        RuleSetEvaluation e = eval();
        Graph g = e.outputGraph();
        Stream<Triple> stream = g.find(s, p, o).toList().stream();
        return stream;
    }

    @Override
    public Graph infer() {
        RuleSetEvaluation e = eval();
        return e.inferredTriples();
    }

    @Override
    public RuleSetEvaluation eval() {

        // dataGraph -- base graph and inferred triples.
        // The graph for the algorithm.
        AppendGraph dataGraph = AppendGraph.create(baseGraph);

        // == DATA
        Graph data = ruleSet.getData() ;
        if ( ruleSet.hasData() ) {
            GraphUtil.addInto(dataGraph, data);
        }

        int round = 0;

        /*
         * dataGraph is a combination of baseGraph, inferred triples, including DATA triples.
         *
         * graph1 is updated by rules in RuleExec.evalRule.
         * It starts being baseGraph+DATA and becomes the output graph.
         * if flushAfterEachRound, write back to dataGraph after each round
         * otherwise
         *
         * accumulationGraph (informational, for development) is all inferred triples
         * and updated either at the end of a round or end of execution.
         * It does not include DATA triples.
         * It is primarily for development and maybe removed
         *
         * graph1
         */

        // Per layer accumulation of inferred triples.
        AppendGraph graph1 = AppendGraph.create(dataGraph);

        // Accumulator graph. New triples.
        // Does not include the DATA.
        // Primarily for development debugging.
        Graph accumulationGraph = GraphFactory.createGraphMem();

        accumulationGraph.getPrefixMapping().setNsPrefixes(dataGraph.getPrefixMapping());

        // True - write back each round.
        // False - accumulate new triples.
        boolean flushAfterEachRound = true;

        // == Rules
        while(true) {
            round++;
            int sizeAtRoundStart = graph1.getAdded().size();

            if ( TRACE )
                System.out.println("Round: "+round);

            for (Rule rule : ruleSet.getRules() ) {
                evalOneRule(graph1, rule, ruleSet.getPrefixMap());

                if ( TRACE )
                    System.out.println("Accumulator: "+graph1.getAdded().size());
            }

            int sizeAtRoundEnd = graph1.getAdded().size();
            if ( sizeAtRoundStart == sizeAtRoundEnd ) {
                // No new triples this round.
                --round;
                // Finished.
                break;
            }

            // END of round.

            if ( flushAfterEachRound ) {
                // Record inferred.
                GraphUtil.addInto(accumulationGraph, graph1.getAdded());
                // Write to working data graph.
                graph1.flush();
            }

            // Whether to write base graph and clear while running.
        }

        if ( ! flushAfterEachRound ) {
            GraphUtil.addInto(accumulationGraph, graph1.getAdded());
            graph1.flush();
        }

        Graph inferred = dataGraph.getAdded();
        inferred.getPrefixMapping().setNsPrefixes(dataGraph.getPrefixMapping());

        return new Evaluation(baseGraph, ruleSet, dataGraph.getAdded(), dataGraph);
    }

    /**
     * One execution of one rules.
     * The argument graph is updated.
     */
    private void evalOneRule(Graph graph, Rule rule, PrefixMap pmap) {
        if ( TRACE ) {
            System.out.print("Rule: ");
            String rs = ShaclRulesWriter.asString(rule, pmap);
            System.out.print(rs);
            //System.out.println();
        }
        List<Triple> triples = RuleExecLib.evalRule(graph, rule);
        GraphUtil.add(graph, triples);
    }
}
