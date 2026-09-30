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
import java.util.function.Supplier;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.seaborne.jena.srl.nexpr.RuleExprEvalException;

/**
 * A {@link GroupReducer}
 */
public /*abstract*/ class GroupReducer {

    private final String name;
    private final Var aggVar;
    // This may throw an ExprEvalE is "empty " is not acceptable.
    private final Supplier<Node> onEmpty;

    private final Map<GroupKey, AggregateFunction> collector = new HashMap<>();
    private final GroupSplitter splitter;

    // Map + factory? vs all one function

    private final AggregateFunction.Factory aggregatorFactory;


    private Map<GroupKey, Node> unused_results = null;
    private List<Binding> rows = null;

    protected GroupReducer(String name, Var outputVar, Supplier<Node> onEmpty, GroupSplitter groupSplitter, AggregateFunction.Factory aggregatorFactory) {
        this.name = name;
        this.aggVar = outputVar;
        this.onEmpty = onEmpty;
        this.splitter = groupSplitter;
        this.aggregatorFactory = aggregatorFactory;
    }

    public void startReceive() {}

    public void receive(Binding binding) {
        GroupKey groupKey = splitter.groupKey(binding);

        if ( groupKey == null ) {
            System.err.println("Null group key");
            throw new RuleExprEvalException("Null group key: "+binding);
        }
        AggregateFunction aggregator =
                collector.computeIfAbsent(groupKey,
                                          k->aggregatorFactory.newAggregateFunction(k));
        aggregator.receive(binding);
        //System.out.println("GroupReducer: "+groupKey+" : "+binding);
    }

    public void receiveEmpty(Binding inputBinding) {
        Node groupEmptyResult = onEmpty.get();
        if ( groupEmptyResult == null )
            throw new ExprEvalException("Empty group/no value");
        Binding resultRow = BindingBuilder.create(inputBinding).add(aggVar, groupEmptyResult).build();

    }

    public void finishReceive() {
        if ( collector.isEmpty() ) {
            // No group key so the result is just the aggregate variable.
            Node emptyValue = onEmpty.get();
            BindingBuilder builder = BindingBuilder.create();
            builder.add(aggVar, emptyValue);
            Binding b = builder.build();
            rows = List.of(b);
            return;
        }
        rows = evalAgg(collector);
    }

    private List<Binding> evalAgg(Map<GroupKey, AggregateFunction> collector) {
        Map<GroupKey, Node> results = new HashMap<>();
        collector.keySet().forEach(key->{
            // Project group keys.
            Node v = collector.get(key).aggNode();
            results.put(key, v);
        });
        List<Binding> rows = new ArrayList<>();
        results.forEach((gk,v)->{
            // Reuse
            BindingBuilder builder = BindingBuilder.create();
            gk.addToBinding(builder);
            builder.add(aggVar, v);
            Binding b = builder.build();
            //System.out.println("  "+b);
            rows.add(b);
        });
        return rows;
    }

    public Iterator<Binding> eval() {
        return rows.iterator();
    }

    String name() { return name; }
    //boolean isDistinct() { return isDistinct; }
}