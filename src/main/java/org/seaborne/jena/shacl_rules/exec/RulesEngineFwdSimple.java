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

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.PrefixMap;
import org.seaborne.jena.shacl_rules.*;
import org.seaborne.jena.shacl_rules.jena.AppendGraph;
import org.seaborne.jena.shacl_rules.sys.Stratification;

/**
 * A simple rules engine that can be easily understood.
 * <p>
 * This is used for testing by running an engine under test and this
 * engine then comparing the results.
 * <p>
 * Supports: recursion, negation.
 */
public class RulesEngineFwdSimple implements RulesEngine {

    private boolean TRACE = false;
    @Override
    public RulesEngineFwdSimple setTrace(boolean traceSetting) {
        TRACE = traceSetting;
        return this;
    }

    public static RulesEngineFwdSimple build(Graph graph, RuleSet ruleSet) {
        RuleExecLib.prepare(ruleSet);
        return new RulesEngineFwdSimple(graph, ruleSet);
    }

    private final RuleSet ruleSet;
    private final Graph baseGraph;

    private RulesEngineFwdSimple(Graph baseGraph, RuleSet ruleSet) {
        this.baseGraph = baseGraph;
        this.ruleSet = ruleSet;
    }

    @Override
    public EvalAlgorithm engineType() {
        return EvalAlgorithm.FWD_NAIVE;
    }

    @Override
    public Graph baseGraph() {
        return baseGraph;
    }

    @Override
    public Graph materializedGraph() {
        Evaluation e = eval();
        return e.outputGraph();
    }

    @Override
    public RuleSet ruleSet() {
        return ruleSet;
    }

    private PrefixMap prefixMap() {
        return ruleSet.getPrefixMap();
    }

    /**
     * This function calculates by all triples, then matches the pattern given.
     */

    @Override
    public Stream<Triple> solve(Node s, Node p, Node o) {
        // Rather than cache, wrap in a "materialize and match" RulesEngine.
        Evaluation e = eval();
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
    public Evaluation eval() {
        // Temporary - this has already been done.
        Stratification stratification = Stratification.create(ruleSet);
        int N = stratification.maxStratum();

        // == dataGraph -- base graph + data.
        // The input graph for the algorithm.
        AppendGraph dataGraph = AppendGraph.create(baseGraph);
        // Add DATA
        Graph ruleSetData = ruleSet.getData() ;
        if ( ruleSet.hasData() ) {
            GraphUtil.addInto(dataGraph, ruleSetData);
        }

        // Prefixes for the inferred graph.
        // === Graph of new triples.
        // Initially, the DATA triples.
        Graph inferred = dataGraph.getAdded();
        inferred.getPrefixMapping().setNsPrefixes(dataGraph.getPrefixMapping());

        IndentedWriter out = IndentedWriter.stdout.clone();

        if ( TRACE ) {
            out.println("Base graph: size = "+baseGraph.size());
            out.println("Inferred graph: size = "+inferred.size());
            //out.println("Inferred graph: size = "+inferred.size());
        }

        try ( out ) {
            for ( int i = 0 ; i <= N ; i++ ) {
                List<Rule> rules = stratification.getLevel(i);
                if ( TRACE ) {
                    out.printf("Level %d -- %d rules\n", i, rules.size());
                    out.incIndent();
                }
                int rounds = evalStratum(i, rules, dataGraph, out);

                if ( TRACE ) {
                    out.println("Base graph: size = "+baseGraph.size());
                    out.println("Inferred graph: size = "+inferred.size());
                    //out.println("Inferred graph: size = "+inferred.size());
                }

                if ( TRACE )
                    out.decIndent();
            }
        } finally { out.flush(); }

        return new Evaluation(baseGraph, ruleSet, dataGraph.getAdded(), dataGraph);
    }

    /* Return the number of of the last round that causes more triples */
    private int evalStratum(int stratumNumber, List<Rule> rules, Graph dataGraph, IndentedWriter out) {
//        if ( TRACE )
//            out.printf("Eval level -- %d rules\n", rules.size());

//        if ( TRACE ) {
//            out.printf("Level %d\n", stratumNumber);
//            out.incIndent();
//        }

//        if ( TRACE ) {
//            out.decIndent();
//        }

        /*
         * dataGraph is a combination of baseGraph, inferred triples, including DATA triples.
         */
        /*
         * graph1 is updated by rules in RuleExec.evalRule.
         * It starts being baseGraph+DATA and becomes the output graph.
         * If "flushAfterEachRound", write back to dataGraph after each round
         * otherwise accumulate over each round.
         * If "flushAfterEachLevel", write back at the end of evalStratum.
         * otherwise accumulate over each round.
         */

        AppendGraph graph1 = AppendGraph.create(dataGraph);

//        /*
//         * accumulationGraph (informational, for development) is all inferred triples
//         * and updated either at the end of a round or end of execution.
//         * It does not include DATA triples.
//         * It is primarily for development and maybe removed
//         */
//        Graph accumulationGraph = GraphFactory.createGraphMem();
//        accumulationGraph.getPrefixMapping().setNsPrefixes(dataGraph.getPrefixMapping());

        // One or the other must be true in order to expose the stratum changes.
        final boolean flushAfterEachRound = true;
        final boolean flushAfterEachLevel = false;
        int round = 0;

        // == Rules
        while(true) {
            round++;
            int sizeAtRoundStart = graph1.getAdded().size();

            if ( TRACE ) {
                out.println("Round: "+round);
                out.incIndent();
            }

            // Evaluate one round.
            // This is the "naive" algorithm.
            // BY tracking rules that actually cause change, we can get semi-naive.

            for (Rule rule : rules ) {
                executeOneRule(graph1, rule, prefixMap(), out);

                if ( TRACE )
                    out.println("Accumulator: "+graph1.getAdded().size());
            }

            if ( TRACE )
                out.decIndent();

            int sizeAtRoundEnd = graph1.getAdded().size();
            if ( sizeAtRoundStart == sizeAtRoundEnd ) {
                // No new triples this round.
                --round;
                // Finished.
                break;
            }

            // END of round.

            if ( flushAfterEachRound )
                flush(graph1);
        }

        // END of execution for this list of rules.
        if ( flushAfterEachLevel && ! flushAfterEachRound )
            // If flushAfterEachRound is true, then exiting
            // the last round did a flush.
            flush(graph1);
        return round;
    }

    private void flush(AppendGraph srcGraph) {
        srcGraph.flush();
    }

    /**
     * One execution of one rules.
     * The argument graph is updated.
     */
    private void executeOneRule(Graph graph, Rule rule, PrefixMap pmap, IndentedWriter out) {
        if ( TRACE ) {
            out.print("Rule: ");
            String rs = ShaclRulesWriter.asString(rule, pmap);
            out.print(rs);
            //out.println();
        }
        List<Triple> triples = RuleExecLib.evalRule(graph, rule);
        GraphUtil.add(graph, triples);
    }
}
