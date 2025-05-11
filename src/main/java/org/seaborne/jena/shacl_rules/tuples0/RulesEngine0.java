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

package org.seaborne.jena.shacl_rules.tuples0;

import java.util.stream.Stream;

import org.apache.jena.sparql.engine.binding.Binding;
import org.seaborne.jena.shacl_rules.EngineType;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.tuples0.rel.Rel;
import org.seaborne.jena.shacl_rules.tuples0.rel.RelStore;

public interface RulesEngine0 {

    interface Factory { RulesEngine0 build(RelStore data, RuleSet ruleSet); }

    public EngineType engineType();

    /**
     * Return all bindings that satisfy the query atom, given the data and {@link RuleSet}.
     */
    public Stream<Binding> solve(Rel query);

    /**
     * Return a {@link Stream} of all derived relationships. It does not include
     * the data unless a data term is also derived by the rules. This may contain
     * duplicates because an atom may be inferred by multiple routes. The
     * multiplicity of duplicates is not significant and it should not be taken as a
     * indication of how many ways there are of generation the atom because the engine
     * is entitled to optimize execution.
     */
    public Stream<Rel> stream();

    /**
     * Return a {@link RelStore} that contains the data and all relationship derived
     * from the rules
     */
    public RelStore materialize();

    /**
     * Note an update to the data has happened
     * (Optional operation).
     */
    public default void update() {}
}
