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

import java.util.Map;

import org.apache.jena.atlas.lib.Registry;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesEngine;

public class RulesEngineRegistry {

    // System setup.
    private static Map<EngineType, RulesEngineFactory> config =
            Map.of(EngineType.SIMPLE, RulesEngineFwdSimple.factory);

    private static RulesEngineRegistry system = new RulesEngineRegistry(config);

    public static  RulesEngineRegistry get() {
        return system;
    }

    private Registry<EngineType, RulesEngineFactory> registry = new Registry<>();

    private RulesEngineRegistry(Map<EngineType, RulesEngineFactory> setup) {
        setup.forEach(registry::put);
    }

    /** Create a RulesEngine */
    public RulesEngine create(EngineType engineType, Graph dataGraph, RuleSet ruleSet, Context context) {
        RulesEngineFactory f = registry.get(engineType);
        if (f == null)
            return null;
        return f.create(dataGraph, ruleSet, context.copy());
    }

}
