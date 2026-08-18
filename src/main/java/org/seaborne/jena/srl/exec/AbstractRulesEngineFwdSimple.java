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

package org.seaborne.jena.srl.exec;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.sparql.graph.GraphReadOnly;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.srl.*;
import org.seaborne.jena.srl.examine.Examine;
import org.seaborne.jena.srl.jena.AppendGraph;
import org.seaborne.jena.srl.sys.Stratification;
import org.seaborne.jena.srl.sys.Stratum;
import org.seaborne.jena.srl.tuples.TupleStore;

/**
 * A framework for simple rules engines
 */
public abstract class AbstractRulesEngineFwdSimple implements RulesEngine {

    protected final RuleSet ruleSet;
    // The input graph. This does not contain the data block triples.
    private final Graph baseGraph;
    private final TupleStore baseTupleStore;
    private final RulesExecCxt rCxt;

    protected AbstractRulesEngineFwdSimple(Graph baseGraph, TupleStore tupleStore, RuleSet ruleSet, RulesExecCxt rCxt) {
        this.baseGraph = baseGraph;
        this.ruleSet = ruleSet;
        this.baseTupleStore = tupleStore;
        this.rCxt = rCxt;
    }

    private boolean TRACE = false;
    @Override
    public AbstractRulesEngineFwdSimple setTrace(boolean traceSetting) {
        TRACE = traceSetting;
        return this;
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
        RuleSetEvaluation e = eval();
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
        return evalRuleSet();
    }

    private RuleSetEvaluation evalRuleSet() {
        if ( TRACE ) {
            ruleSet.getRules().forEach(rule->{
                String s = ShaclRulesWriter.asString(rule, ruleSet.getPrefixMap());
                rCxt.out().printf("%s %s", ruleSet.labelFor(rule), s);
            });
        }

        Stratification stratification = RulesExecLib.prepare(ruleSet, rCxt);

        int maxStratum = stratification.maxStratum(); // Inclusive.

        // NOW()
        Context.setCurrentDateTime(rCxt.getContext());

        TRACE = TRACE || rCxt.trace();

        // == inputGraph -- base graph
        //   Immutable
        // == evalGraph -- the working graph of the algorithm
        //   It grows as execution proceeds.

        Graph inputGraph = readOnly(baseGraph);

        AppendGraph evalGraph = AppendGraph.create(baseGraph);
        // Add DATA to seed the evaluation graph
        if ( ruleSet.hasData() )
            GraphUtil.addInto(evalGraph, ruleSet.getData());

        // rCxt.strict
        TupleStore tupleStore = TupleStore.create();
        if ( ruleSet.hasTupleData() || baseTupleStore != null ) {
            if ( ruleSet.hasTupleData() )
                tupleStore.addAll(ruleSet.getDataTuples());
            if ( baseTupleStore != null )
                tupleStore.addAll(baseTupleStore);
        }

        // Prefixes for the inferred graph.
        // === View graph of new triples.
        Graph inferredGraph = evalGraph.getAdded();
        inferredGraph.getPrefixMapping().setNsPrefixes(Prefixes.adapt(ruleSet.getPrefixMap()));
        inferredGraph.getPrefixMapping().setNsPrefixes(baseGraph.getPrefixMapping());

        if ( TRACE ) {
            rCxt.out().println("Base graph: size = "+baseGraph.size());
            rCxt.out().println("Initial inferred graph: size = "+inferredGraph.size());
        }

        return evalStratification(evalGraph, inputGraph, stratification, tupleStore);
    }

    private RuleSetEvaluation evalStratification(AppendGraph evalGraph, Graph inputGraph, Stratification stratification, TupleStore tupleStore) {

        if ( Examine.EXAMINE )
            rCxt.out().println("==== Evaluation");

        try {
            for ( int i = stratification.minStratum() ; i <= stratification.maxStratum() ; i++ ) {
                Stratum stratum = stratification.getLevel(i);
                if ( Examine.EXAMINE || TRACE ) {
                    rCxt.out().printf("Level %d -- (Once=%d, General=%d) rules\n", i, stratum.runOnce().size(), stratum.runGeneral().size());
                    rCxt.out().incIndent();
                    rCxt.out().flush();
                }
                int rounds = evalStratum(evalGraph, i, stratum, inputGraph, tupleStore, rCxt);

                if ( TRACE ) {
                    //rCxt.out().println("Base graph: size = "+baseGraph.size());
                    rCxt.out().println("Inferred graph: size = "+evalGraph.getAdded().size());
                }

                if ( Examine.EXAMINE || TRACE )
                    rCxt.out().decIndent();
            }
        } finally {
            rCxt.out().flush(); }

        return new Evaluation(baseGraph, ruleSet, evalGraph.getAdded(), evalGraph, tupleStore);
    }

    /* Return the number of the last round that causes more triples */
    private int evalStratum(Graph evalGraph, int stratumNumber, Stratum stratum, Graph inputGraph, TupleStore evalTupleStore, RulesExecCxt rCxt) {
//        if ( TRACE )
//            rCxt.out().printf("Eval level -- %d rules\n", rules.size());

//        if ( TRACE ) {
//            rCxt.out().printf("Level %d\n", stratumNumber);
//            rCxt.out().incIndent();
//        }
// ...
//        if ( TRACE ) {
//            rCxt.out().decIndent();
//        }

        /*
         * graph1 is updated by rules in RuleExec.evalRule.
         * It starts being baseGraph+DATA and becomes the output graph.
         * If "flushAfterEachRound", write back to evalGraph after each round
         * otherwise accumulate over each round.
         * If "flushAfterEachLevel", write back at the end of evalStratum.
         * otherwise accumulate over each round.
         */

        AppendGraph graph1 = AppendGraph.create(evalGraph);

        Collection<Rule> runOnceRules = stratum.runOnce();
        Collection<Rule> runGeneralRules = stratum.runGeneral();

        // == Run once
        if ( !runOnceRules.isEmpty() ) {
            if ( TRACE ) {
                rCxt.out().println("Run once: "+runOnceRules.size());
                rCxt.out().incIndent();
            }
            for ( Rule rule : runOnceRules ) {
                if ( TRACE )
                    System.out.printf("Eval(once): %s\n", ruleSet.labelFor(rule));
                executeRule(graph1, evalTupleStore, inputGraph, rule, rCxt);
                if ( TRACE )
                    rCxt.out().println("Accumulator: "+graph1.getAdded().size());
            }
            flush(graph1);

            if ( TRACE )
                rCxt.out().decIndent();
        }

        // One or the other must be true in order to expose the stratum changes.
        final boolean flushAfterEachRound = true;
        final boolean flushAfterEachLevel = false;

        int round = 0;

        // == Run all
        while(true) {
            round++;
            int sizeAtRoundStart = graph1.getAdded().size() + evalTupleStore.size();

            if ( TRACE ) {
                rCxt.out().println("Round: "+round);
                rCxt.out().incIndent();
            }

            // Evaluate one round.
            // This is the "naive" algorithm.
            // By tracking rules that actually cause change, we can get semi-naive.

            for ( Rule rule : runGeneralRules ) {
                if ( TRACE )
                    rCxt.out().printf("Eval(general): round=%d : %s\n", round, ruleSet.str(rule));
                executeRule(graph1, evalTupleStore, inputGraph, rule, rCxt);

                if ( TRACE )
                    rCxt.out().println("Accumulator: "+graph1.getAdded().size());

                if ( Examine.EXAMINE ) {
                    int added = graph1.getAdded().size();
                    String count = added == 0 ? "*" : Integer.toString(added);
                    rCxt.out().printf("Eval: round=%d : triples=%s : %s\n", round, count, ruleSet.str(rule));
                }
            }

            if ( TRACE )
                rCxt.out().decIndent();

            int sizeAtRoundEnd = graph1.getAdded().size() + evalTupleStore.size();
            if ( sizeAtRoundStart == sizeAtRoundEnd ) {
                // No new triples or tuples this round.
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
        if ( TRACE )
            rCxt.out().flush();
        return round;
    }

    private void flush(AppendGraph srcGraph) {
        srcGraph.flush();
    }

    // Helper - wrap in a graph implementation that blocks add/delete.
    private static Graph readOnly(Graph graph) {
       return new GraphReadOnly(graph);
    }

    private void executeRule(Graph evalGraph, TupleStore evalTupleStore, Graph inputGraph, Rule rule, RulesExecCxt rCxt) {
        Objects.requireNonNull(evalGraph);
        Objects.requireNonNull(rule);
        Objects.requireNonNull(inputGraph);
        Objects.requireNonNull(rCxt);
        // Readonly wrappers for checking.
        executeOneRule(evalGraph, evalTupleStore, inputGraph, rule, rCxt);
    }

    /**
     * One execution of one rule.
     * The arguments evalGraph and evalTupleStore are updated.
     */
    protected abstract void executeOneRule(Graph evalGraph, TupleStore evalTupleStore, Graph inputGraph, Rule rule, RulesExecCxt rCxt);
}
