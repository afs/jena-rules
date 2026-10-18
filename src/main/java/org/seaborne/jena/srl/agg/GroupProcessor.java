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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.seaborne.jena.srl.nexpr.RuleExprEvalException;

/**
 * A {@link GroupProcessor} takes the stream of pattern matches (bindings)
 * splits them into groups, and passes the binds to the aggregation functions.
 * At he end it produce the output.
 */
public class GroupProcessor {
    // A group key to the list of aggregators for that group.
    private final ListValuedMap<GroupKey, Aggregator> groups = new ArrayListValuedHashMap<>();
    private final GroupSplitter splitter;
    private final List<AggregatorFactory> aggregatorFactories;
    private List<Binding> rows;
    private Binding currentRow = null;

    public GroupProcessor(GroupSplitter groupSplitter, List<AggregatorFactory> aggregatorFactories) {
        this.splitter = groupSplitter;
        this.aggregatorFactories = aggregatorFactories;
    }

    /** Called at the start of reduction */
    public void startReceive() {}

    /**
     * Reset for next row.
     * Having this, rather than a new object allows for caching in this GroupProcessor object.
     */
    public void nextRow(Binding row) {
        this.currentRow = row;
        groups.clear();
    }

    public void receive(Binding binding) {
        GroupKey groupKey = splitter.groupKey(binding);


        if ( groupKey == null ) {
            System.err.println("Null group key");
            throw new RuleExprEvalException("Null group key: "+binding);
        }

        List<Aggregator> aggregators = groups.get(groupKey);
        if ( aggregators == null || aggregators.isEmpty()) {
            for ( AggregatorFactory factory : aggregatorFactories ) {
                var agg = factory.newAggregator(groupKey);
                agg.start();
                groups.put(groupKey, agg);
                aggregators = groups.get(groupKey);
            }
        }

        for ( Aggregator agg : aggregators  ) {
            agg.receive(binding);
        }
        //System.out.println("GroupProcessor: "+groupKey+" : "+binding);
    }

    public void finishReceive() {
        if ( groups.isEmpty() ) {
            aggregatorFactories.forEach(aggMgr->{
              // May throw an ExprEvalException
              Node emptyValue = aggMgr.onEmpty();
              BindingBuilder builder = BindingBuilder.create(currentRow);
              builder.add(aggMgr.assignVar(), emptyValue);
              Binding b = builder.build();
              rows = List.of(b);
            });
            return;
        }
        rows = evalAgg(groups, currentRow);
    }

    // "parent" is the binding coming in from before the GROUP
    private static List<Binding> evalAgg(ListValuedMap<GroupKey, Aggregator> groups, Binding parent) {
        // For each group, for each aggregator, calculate the result
        List<Binding> rows = new ArrayList<>();

        // System.out.println("Group keys: "+groups.keySet().size());

        groups.keySet().forEach(gk->{
            BindingBuilder builder = (parent != null)
                    ? BindingBuilder.create(parent)
                    : BindingBuilder.create(BindingBuilder.noParent);
            // May or may not be present in the parent.
            gk.addToBinding(builder);

            for ( Aggregator agg : groups.get(gk) ) {
                agg.finish();
                Node n = agg.aggNode();
                Var v = agg.assignVar();
                builder.add(v,n);
            }
            Binding row = builder.build();
            rows.add(row);
        });
        return rows;
    }

    public Iterator<Binding> eval() {
        return rows.iterator();
    }
}