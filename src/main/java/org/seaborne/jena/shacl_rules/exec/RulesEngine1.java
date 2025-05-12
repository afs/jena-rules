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
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sparql.exec.RowSetRewindable;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.system.buffering.BufferingGraph;
import org.seaborne.jena.shacl_rules.*;
import org.seaborne.jena.shacl_rules.jena.AppendGraph;

public class RulesEngine1 implements RulesEngine {
    public static boolean verbose = false;

    interface Factory { RulesEngine1 build(/*RelStore data, */RuleSet ruleSet); }

    public static RulesEngine build(RuleSet ruleSet) {
        return new RulesEngine1(ruleSet);
    }

    private final RuleSet ruleSet;


    private RulesEngine1(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }


    @Override
    public EngineType engineType() {
        return EngineType.FWD_NAIVE;
    }

    // This function calculates by all methods (accumulator graph (new triples), updates base graph (maybe copy-isolated), retain buffering graph)
    // Specialise later.

    @Override
    public Stream<Triple> find(Graph baseGraph, Node s, Node p, Node o) {
        // The heavy-handed way!
        Evaluation e = eval(baseGraph, false);
        Graph g = e.outputGraph();
        Stream<Triple> stream = g.find(s, p, o)
            .toList()   // Materialize
            .stream();
        return stream;
    }

    @Override
    public Graph infer(Graph baseGraph) {
        Evaluation e = eval(baseGraph, false);
        return e.inferredTriples;
    }

    public record Evaluation(Graph originalGraph, Graph inferredTriples, Graph outputGraph, int rounds) {}

    // Algorithm for development - captures more than is needed.

    public Evaluation eval(Graph baseGraph) {
        return eval(baseGraph, false);
    }

    public Evaluation eval(Graph baseGraph, boolean verbose) {

        boolean updateBsseGraph = false;

        // Needs improvement : Copy baseGraph, and update copy.
        // The graph for the algorithm. Updated.
        Graph dataGraph = R.cloneGraph(baseGraph);

        Graph data = ruleSet.getData() ;
        // Recalculate the basegraph and make it look like data was added.
        if ( ruleSet.hasData() ) {
            dataGraph = new AppendGraph(dataGraph);
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

            if ( verbose )
                System.out.println("Round: "+round);

            for (Rule rule : ruleSet.getRules() ) {
                if ( verbose )
                    System.out.println("Rule: "+rule);
                // graph1 vs graph
                RowSetRewindable rowset = evalRule(graph1, ruleSet.getPrologue(), rule).rewindable();

                if ( verbose ) {
                    RowSetOps.out(rowset);
                    rowset.reset();
                }

                BasicPattern bgp = rule.getHead();
                rowset.forEach(row->{
                    BasicPattern bgp2 = Substitute.substitute(bgp, row);
                    bgp2.forEach(t->graph1.add(t));
                });
                if ( verbose )
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

            if ( verbose )
                System.out.println();
            // Whether to write base graph and clear while running.
        }

        if ( ! flushAfterEachRound ) {
            GraphUtil.addInto(accGraph, graph1.getAdded());
            graph1.flush();
        }

        return new Evaluation(baseGraph, accGraph, dataGraph, round);
    }

    private static RowSet evalRule(Graph graph, Prologue prologue, Rule rule) {
        ElementGroup eltGroup = rule.getBody();
        Query query = rule.bodyAsQuery();
        BasicPattern bgp = rule.getHead();
        RowSet rowset = QueryExec.graph(graph).query(query).select();
        return rowset;
    }

}
