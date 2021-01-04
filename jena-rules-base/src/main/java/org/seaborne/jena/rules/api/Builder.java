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

package org.seaborne.jena.rules.api;

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.seaborne.jena.rules.RelStore;
import org.seaborne.jena.rules.RuleSet;
import org.seaborne.jena.rules.RulesEngine;
import org.seaborne.jena.rules.RulesException;
import org.seaborne.jena.rules.exec.RulesEngineSLD_NR;
import org.seaborne.jena.rules.exec.RulesGraph;
import org.seaborne.jena.rules.naive.RuleEngineNaive;
import org.seaborne.jena.rules.store.RelStoreGraph;

public class Builder {
    private Context context = null;
    private Graph baseGraph;
    private RuleSet ruleSet;
    private EngineType engineType;

    public Builder() {}

    public Builder set(Symbol symbol, Object value) {
        ensureContext();
        return this;
    }

    public Builder baseGraph(Graph graph) {
        this.baseGraph = graph;
        return this;
    }

    public Builder rules(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
        return this;
    }

    public Builder rules(String filename) {
        this.ruleSet = Rules.rules(filename);
        return this;
    }

    public Builder system(EngineType type) {
        this.engineType = type;
        return this;
    }

    public Graph build() {
        if ( baseGraph == null )
            throw new RulesException("No base data graph provided");
        if ( ruleSet == null )
            throw new RulesException("No rule set provided");

        Context cxt = context;
        if ( cxt == null )
            context = Rules.getContext();

        EngineType type = (this.engineType == null) ? this.engineType : EngineType.BKD_NON_RECURSIVE_SLD;
        RulesEngine.Factory factory = null;
        switch(type) {
            case BKD_NON_RECURSIVE_SLD :
                factory = RulesEngineSLD_NR.factory;
                break;
            case BKD_QSQI :
            case BKD_QSQR :

            case FWD_NAIVE :
                factory = RuleEngineNaive.factory;
                break;
            case FWD_NAIVE_GUEASS_SEIDEL:
            case FWD_NAIVE_JACOBI:
            case FWD_SEMINAIVE :
                break;
            default :
                break;
        }

        if (factory == null )
            throw new RulesException("No factory for "+type);

        RelStore edb = new RelStoreGraph(baseGraph);
        RulesEngine engine = factory.build(edb, ruleSet);
        Graph graph = new RulesGraph(baseGraph, engine);
        return graph;
    }

    //private static

    private void ensureContext() {
        if ( context == null )
            context = new Context();
    }
}
