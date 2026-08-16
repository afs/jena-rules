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

import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.RulesEngine;
import org.seaborne.jena.srl.ShaclRulesExec;
import org.seaborne.jena.srl.tuples.TupleStore;

/**
 * A simple rules engine that translates the body to a SPARQL query.
 * <p>
 * Supports: SRL (recursion. negation, run-once)
 * Does not support: tuples
 */
public class RulesEngineFwdSimpleSparqlConstruct extends AbstractRulesEngineFwdSimple implements RulesEngine {

    public static final RulesEngineFactory factory = RulesEngineFwdSimpleSparqlConstruct::build;

    /**
     * Not public.
     * Preferred: use {@link ShaclRulesExec#create(EngineType, Graph, TupleStore, RuleSet)}
     * with {@link EngineType#SIMPLE} which goes via the RulesEngineRegistry
     */
    private
    static RulesEngine build(Graph baseGraph, TupleStore tupleStore, RuleSet ruleSet, Context cxt) {
        RulesExecCxt rCxt = RulesExecCxt.create(cxt);
        return new RulesEngineFwdSimpleSparqlConstruct(baseGraph, tupleStore, ruleSet, rCxt);
    }

    private RulesEngineFwdSimpleSparqlConstruct(Graph baseGraph, TupleStore tupleStore, RuleSet ruleSet, RulesExecCxt rCxt) {
        super(baseGraph, tupleStore, ruleSet, rCxt);
    }

    /**
     * One execution of one rule.
     * The argument graph is updated.
     */
    @Override
    protected void executeOneRule(Graph evalGraph, TupleStore evalTupleStore, Graph dataGraph, Rule rule, RulesExecCxt rCxt) {
        // Via CONSTRUCT
        Query query = RulesLibSparql.ruleToConstruct(rule);
        DatasetGraph dsg = RulesLibSparql.buildDataset(evalGraph, dataGraph);

        Iterator<Triple> triples = QueryExec.dataset(dsg)
                .query(query)
                .context(rCxt.getContext())
                .build()
                .constructTriples();
        GraphUtil.add(evalGraph, triples);
    }
}
