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

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.seaborne.jena.srl.nexpr.RuleExprEvalException;

// Is this the "reducer"?
public /*abstract*/ class Reducer {

    private final String name;
    private final boolean isDistinct;
    private final Var aggVar;
    // This may throw an ExprEvalE is "empty " is not acceptable.
    private final Supplier<Node> onEmpty;

    private final MultiValuedMap<GroupKey, Binding> collector = new ArrayListValuedHashMap<>();
    private final GroupSplitter splitter;
    private final Function<Collection<Binding>, Node> groupValue;
    private Map<GroupKey, Node> unused_results = null;
    private List<Binding> rows = null;

    protected Reducer(String name, boolean isDistinct, Var outputVar, Supplier<Node> onEmpty, GroupSplitter groupSplitter, Function<Collection<Binding>, Node> groupValue) {
        this.name = name;
        this.aggVar = outputVar;
        this.onEmpty = onEmpty;
        this.isDistinct = isDistinct;
        this.splitter = groupSplitter;
        this.groupValue = groupValue;
    }

    public void startReceive() {}

    public void receive(Binding binding) {
        GroupKey groupKey = splitter.groupKey(binding);

        if ( groupKey == null ) {
            System.err.println("Null group key");
            throw new RuleExprEvalException("Null group key: "+binding);
        }
        if ( isDistinct ) {
            // DISTINCT??
            // XXX What is the right data structure to use here?
            if ( collector.containsMapping(groupKey, binding) )
                return;
        }
        //System.out.println("put: "+groupKey+" : "+binding);
        collector.put(groupKey, binding);
    }

    public void receiveEmpty(Binding inputBinding) {
        Node groupEmptyResult = onEmpty.get();
        if ( groupEmptyResult == null )
            throw new ExprEvalException("Empty group/no value");
        Binding resultRow = BindingBuilder.create(inputBinding).add(aggVar, groupEmptyResult).build();

    }

    public void finishReceive() {
        // XXX
        if ( false ) {
            Map<GroupKey, Collection<Binding>> map = collector.asMap();
            System.err.println("Group: keys="+map.keySet().size());
            map.keySet().forEach(gk->{
                System.err.print("  "+gk);
                System.err.println("  "+map.get(gk));
            });
        }

        Map<GroupKey, Collection<Binding>> map = collector.asMap();
        if ( map.isEmpty() ) {
            // No group key so the result is just the aggregate variable.
            Node emptyValue = onEmpty.get();
            BindingBuilder builder = BindingBuilder.create();
            builder.add(aggVar, emptyValue);
            Binding b = builder.build();
            rows = List.of(b);
            return;
        }
        rows = evalAgg(map);
    }

    private List<Binding> evalAgg(Map<GroupKey, Collection<Binding>> map) {
        Map<GroupKey, Node> results = new HashMap<>();
        map.keySet().forEach(key->{
            Collection<Binding> x = map.get(key);
            // Project group keys.
            Node v = groupValue.apply(x);
            results.put(key, v);
        });
        List<Binding> rows = new ArrayList<>();
        results.forEach((gk,v)->{
            // Reuse
            BindingBuilder builder = BindingBuilder.create();
            gk.addToBinding(builder);
            builder.add(aggVar, v);
            Binding b = builder.build();
            System.out.println("  "+b);
            rows.add(b);
        });
        return rows;
    }

    public Iterator<Binding> eval() {
        return rows.iterator();
    }

    String name() { return name; }
    boolean isDistinct() { return isDistinct; }
}