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

import java.util.Map;

import org.apache.jena.atlas.lib.Registry;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.RulesEngine;
import org.seaborne.jena.srl.sys.SysSRL;
import org.seaborne.jena.srl.tuples.TupleStore;

public class RulesEngineRegistry {

    // System setup.
    private static Map<EngineType, RulesEngineFactory> config = setup();
    private static Map<EngineType, RulesEngineFactory> setup() {
        return
                Map.of(EngineType.SIMPLE, RulesEngineFwdSimple.factory,
                       EngineType.SIMPLE_SPARQL, RulesEngineFwdSimpleSparqlBody.factory,
                       EngineType.SIMPLE_SPARQL_INSERT, RulesEngineFwdSimpleSparqlInsert.factory,
                       EngineType.SIMPLE_SPARQL_CONSTRUCT, RulesEngineFwdSimpleSparqlConstruct.factory
                        );
    }

    public static void init() {
        //config = setup();
    }

    private static RulesEngineRegistry system = new RulesEngineRegistry(config);

    public static  RulesEngineRegistry get() {
        return system;
    }

    private Registry<Symbol, RulesEngineFactory> registry = new Registry<>();

    private RulesEngineRegistry(Map<EngineType, RulesEngineFactory> setup) {
        setup.forEach((eType, factory) -> put(eType, factory));
    }

    public RulesEngineRegistry put(EngineType engineType, RulesEngineFactory factory) {
        return put(engineType.symbol(), factory);
    }


    public RulesEngineRegistry put(Symbol engineType, RulesEngineFactory factory) {
        registry.put(engineType, factory);
        return this;
    }

    /** Create a RulesEngine */
    public RulesEngine create(EngineType engineType, Graph inputGraph, TupleStore tupleStore, RuleSet ruleSet, Context context) {
        return create(engineType.symbol(), inputGraph, tupleStore, ruleSet, context);
    }

    /** Create a RulesEngine */
    public RulesEngine create(Symbol engineType, Graph inputGraph, TupleStore tupleStore, RuleSet ruleSet, Context context) {
        RulesEngineFactory f = registry.get(engineType);
        if (f == null)
            return null;
        Context cxt = (context == null)
                ? SysSRL.getContext().copy()
                : context.copy();
        cxt.remove(ARQConstants.sysCurrentTime);
        cxt.remove(ARQConstants.sysCurrentAlgebra);
        cxt.remove(ARQConstants.sysCurrentQuery);
        return f.create(inputGraph, tupleStore, ruleSet, cxt);
    }
}
