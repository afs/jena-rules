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

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.RulesEngine;
import org.seaborne.jena.srl.ShaclRulesExec;
import org.seaborne.jena.srl.tuples.TupleStore;

/**
 * A simple rules engine that can be easily understood.
 * <p>
 * This is used for testing by running an engine under test and this
 * engine then comparing the results.
 * <p>
 * Supports: SRL: recursion, negation, run-once, tuples.
 */
public class RulesEngineFwdSimple extends AbstractRulesEngineFwdSimple implements RulesEngine {

    public static final RulesEngineFactory factory = RulesEngineFwdSimple::build;

    /**
     * Not public.
     * Preferred: use {@link ShaclRulesExec#create(EngineType, Graph, TupleStore, RuleSet)}
     * with {@link EngineType#SIMPLE} which goes via the RulesEngineRegistry
     */
    private
    static RulesEngine build(Graph graph, TupleStore tupleStore, RuleSet ruleSet, Context cxt) {
        RulesExecCxt rCxt = RulesExecCxt.create(cxt);
        return new RulesEngineFwdSimple(graph, tupleStore, ruleSet, rCxt);
    }

    private RulesEngineFwdSimple(Graph baseGraph, TupleStore tupleStore, RuleSet ruleSet, RulesExecCxt rCxt) {
        super(baseGraph, tupleStore, ruleSet, rCxt);
    }

    /**
     * One execution of one rule.
     * The argument graph is updated.
     */
    @Override
    protected void executeOneRule(Graph matchGraph, TupleStore evalTupleStore, Graph baseGraph, Rule rule, RulesExecCxt rCxt) {
        //sSystem.out.println("Execute: "+rule);
        RuleEval rEval = RulesExecLib.evalRule(rule, matchGraph, evalTupleStore, baseGraph, rCxt);
        RulesExecLib.accumulateOneRuleHead(rEval, matchGraph, evalTupleStore, rCxt);
    }
}
