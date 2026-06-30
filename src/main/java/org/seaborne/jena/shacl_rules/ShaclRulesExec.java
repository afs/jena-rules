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

import java.util.List;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.shacl_rules.exec.EngineType;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.exec.RulesEngineRegistry;
import org.seaborne.jena.shacl_rules.tuples.TupleStore;

/** API - evaluation of a {@link RuleSet} over a {@link Graph baseGraph} */
public class ShaclRulesExec {

    //XXX Transition from RulesEngine statics.
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
        return RulesEngineRegistry.get().create(engineType, graph, null, ruleSet, Rules.getContext());
    }

    /**
     * Create a rules engine for the graph data, tuple data, and the rule set.
     */
    public static RulesEngine create(EngineType engineType, Graph graph, TupleStore tupleData, RuleSet ruleSet) {
        return RulesEngineRegistry.get().create(engineType, graph, tupleData, ruleSet, null);
    }

    /**
     * Create a rules engine for the graph data, tuple data, and the rule set.
     */
    public static RulesEngine create(EngineType engineType, Graph graph, TupleStore tupleData, RuleSet ruleSet, Context context) {
        return RulesEngineRegistry.get().create(engineType, graph, tupleData, ruleSet, context);
    }

    public static ShaclRulesExec.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        Builder() {}

        private EngineType engineType = EngineType.SIMPLE;
        private RuleSet ruleSet = null;
        private Graph baseGraph = null;
        private TupleStore tupleStore = null;
        private Context context = null;


        public Builder engine(EngineType engineType) {
            this.engineType = engineType;
            return this;
        }

        public Builder ruleSet(RuleSet ruleSet) {
            this.ruleSet = ruleSet;
            return this;
        }

        public Builder dataGraph(Graph baseGraph) {
            this.baseGraph = baseGraph;
            return this;
        }

        public Builder tupleStore(TupleStore tupleStore ) {
            this.tupleStore = tupleStore;
            return this;
        }



        public Builder context(Context context) {
            this.context  = context;
            return this;
        }

        public RulesEngine build() {
            require(ruleSet, "Required: ruleset");
            require(engineType, "Required: engineType");

            Context cxt = (context==null) ? Rules.getContext().copy() : context;

            switch (engineType) {
                //case BKD_NON_RECURSIVE ->{}
                case SIMPLE ->{
                    //RuleSetEvaluation e = RulesEngine.create(engineType, baseGraph, tupleStore, ruleSet, context).eval();
                    return RulesEngineRegistry.get().create(engineType, baseGraph, tupleStore, ruleSet, context);
                }
                case null-> throw new InternalErrorException();
                default->{
                    throw new RulesException("Engine type "+engineType.name()+" not currently supported");
                }
            }
        }

        private static void require(Object x, String message) {
            if ( x == null )
                throw new RulesException(message);
        }
    }

    private final RulesEngine engine;

    private ShaclRulesExec(RulesEngine engine) {
        this.engine = engine;
    }



    public RuleSetEvaluation exec() {
        return engine.eval();
    }

    public static RuleSetEvaluation execute(RuleSet ruleSet, Graph baseGraph) {
        RulesEngine srExec = ShaclRulesExec.newBuilder().ruleSet(ruleSet).dataGraph(baseGraph).engine(EngineType.SIMPLE).build();
        return srExec.eval();
    }

    // Pull RulesEngine statics here?

//    public static RuleSetEvaluation execute(RuleSet ruleSet, Graph baseGraph) {
//        RuleSetEvaluation e = RulesEngine.create(baseGraph, ruleSet).eval();
//        return e;
//    }

    /**
     * Execute a rule and return any new triples not in the baseGraph)
     */
    public static Graph execute(Rule rule, Graph baseGraph) {
        RuleSet ruleSet = RuleSet.create(null, null, null, List.of(rule), null, null);
        RuleSetEvaluation e = ShaclRulesExec.create(baseGraph, ruleSet).eval();
        return e.inferredTriples();
    }

}
