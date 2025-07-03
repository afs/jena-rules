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

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sparql.exec.RowSetRewindable;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.buffering.BufferingGraph;
import org.seaborne.jena.shacl_rules.EngineType;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesEngine;
import org.seaborne.jena.shacl_rules.jena.AppendGraph;
import org.seaborne.jena.shacl_rules.jena.JLib;

/**
 * A simple rules engine that can be easily understood.
 * <p>
 * This is used for testing by running an engine under test and this
 * engine then comparing the results.
 */
public class RulesEngine1 implements RulesEngine {

    private boolean TRACE = true;
    @Override
    public RulesEngine1 setTrace(boolean traceSetting) {
        TRACE = traceSetting;
        return this;
    }

    public static RulesEngine1 build(Graph graph, RuleSet ruleSet) {
        return new RulesEngine1(graph, ruleSet);
    }

    private final RuleSet ruleSet;
    private final Graph baseGraph;

    private RulesEngine1(Graph baseGraph, RuleSet ruleSet) {
        this.baseGraph = baseGraph;
        this.ruleSet = ruleSet;
    }

    @Override
    public EngineType engineType() {
        return EngineType.FWD_NAIVE;
    }

    @Override
    public Graph graph() {
        return baseGraph;
    }
    @Override
    public RuleSet ruleSet() {
        return ruleSet;
    }

    // This function calculates by all methods (accumulator graph (new triples), updates base graph (maybe copy-isolated), retain buffering graph)
    // Specialise later.

    @Override
    public Stream<Triple> solve(Node s, Node p, Node o) {
        // The heavy-handed way!
        Evaluation e = eval();
        Graph g = e.outputGraph();
        Stream<Triple> stream = g.find(s, p, o)
            .toList()   // Materialize
            .stream();
        return stream;
    }

    @Override
    public Graph infer() {
        Evaluation e = eval();
        return e.inferredTriples;
    }

    public record Evaluation(Graph originalGraph, Graph inferredTriples, Graph outputGraph, int rounds) {}

    // Algorithm for development - captures more than is needed.

    public Evaluation eval() {

        boolean updateBsseGraph = false;

        // Needs improvement : Copy baseGraph, and update copy.
        // The graph for the algorithm. Updated.
        Graph dataGraph = JLib.cloneGraph(baseGraph);

        Graph data = ruleSet.getData() ;
        // Recalculate the baseGraph and make it look like data was added.
        if ( ruleSet.hasData() ) {
            dataGraph = AppendGraph.create(dataGraph);
            GraphUtil.addInto(dataGraph, data);
        }

        int round = 0;

        BufferingGraph graph1 = new BufferingGraph(dataGraph);

        // Accumulator graph. New triples.
        Graph accGraph = GraphFactory.createGraphMem();

        accGraph.getPrefixMapping().setNsPrefixes(dataGraph.getPrefixMapping());

        // True - write back each round.
        // False - accumulate new triples.
        boolean flushAfterEachRound = true;

        // Long term ...
//        // == Data.
//        if (ruleSet.hasData() ) {
//            GraphUtil.addInto(graph1, data);
//            if ( flushAfterEachRound ) {
//                GraphUtil.addInto(accGraph, graph1.getAdded());
//                graph1.flush();
//            }
//        }

        // == Rules
        while(true) {
            round++;
            int sizeAtRoundStart =  graph1.getAdded().size();

            if ( TRACE )
                System.out.println("Round: "+round);

            for (Rule rule : ruleSet.getRules() ) {
                if ( TRACE )
                    System.out.println("Rule: "+rule);
                // graph1 vs graph
                RowSetRewindable rowset = RuleExec.evalRule(graph1, rule).rewindable();

                if ( TRACE ) {
                    RowSetOps.out(rowset);
                    rowset.reset();
                }

                BasicPattern bgp = rule.getHead().asBGP();
                rowset.forEach(row->{
                    BasicPattern bgp2 = Substitute.substitute(bgp, row);
                    bgp2.forEach(t->graph1.add(t));
                });
                if ( TRACE )
                    System.out.println("Accumulator: "+graph1.getAdded().size());
            }

            int sizeAtRoundEnd = graph1.getAdded().size();
            if ( sizeAtRoundStart == sizeAtRoundEnd ) {
                // No new triples this round.
                --round;
                break;
            }

            // END of round.

            if ( flushAfterEachRound ) {
                // Record inferred.
                GraphUtil.addInto(accGraph, graph1.getAdded());
                // Write to working data graph.
                graph1.flush();
            }

            if ( TRACE )
                System.out.println();
            // Whether to write base graph and clear while running.
        }

        if ( ! flushAfterEachRound ) {
            GraphUtil.addInto(accGraph, graph1.getAdded());
            graph1.flush();
        }

        return new Evaluation(baseGraph, accGraph, dataGraph, round);
    }
}
