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

package org.seaborne.jena.srl.agg;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.seaborne.jena.srl.nexpr.RuleExprEvalException;

// Is this the "reducer"?
public /*abstract*/ class Reducer {

    private final String name;
    private final boolean isDistinct;
    private final Var aggVar;
    // This may throw an ExprEvalE is "empty " is not acceptable.
    private final Supplier<Node> onEmpty;

    private final ListValuedMap<GroupKey, Binding> collector = new ArrayListValuedHashMap<>();
    private final Function<Binding, GroupKey> splitter;
    private final Function<List<Binding>, Node> groupValue;
    private Map<GroupKey, Node> results = null;

    protected Reducer(String name, boolean isDistinct, Var outputVar, Supplier<Node> onEmpty, Function<Binding, GroupKey> groupSplitter, Function<List<Binding>, Node> groupValue) {
        this.name = name;
        this.aggVar = outputVar;
        this.onEmpty = onEmpty;
        this.isDistinct = isDistinct;
        this.splitter = groupSplitter;
        this.groupValue = groupValue;
    }

    public void startReceive() {}

    public void receive(Binding binding) {
        GroupKey groupKey = splitter.apply(binding);
        if ( groupKey == null ) {
            System.err.println("Null group key");
            throw new RuleExprEvalException("Null group key: "+binding);
        }
        // DISTINCT??
        collector.put(groupKey, binding);
    }

    public void finishReceive() {
        results = evalAgg();
    }

    private Map<GroupKey, Node> evalAgg() {
        Map<GroupKey, Node> results = new HashMap<>();
        collector.mapIterator().forEachRemaining(key->{
            List<Binding> x = collector.get(key);
            Node v = groupValue.apply(x);
            results.put(key, v);
        });
        return results;
    }

    // XXXX Merge these two?

    public Iterator<Binding> eval() {
        if ( results.isEmpty() ) {
            Binding row = BindingBuilder.create().add(aggVar, onEmpty.get()).build();
            return Iter.singletonIterator(row);
        }

        List<Binding> rows = new ArrayList<>();
        BindingBuilder builder = BindingBuilder.create();
        results.forEach((gk,v)->{
            gk.addToBinding(builder);
            builder.add(aggVar, v);
        });
        rows.add(builder.build());
        return rows.iterator();

    }
    String name() { return name; }
    boolean isDistinct() { return isDistinct; }
}