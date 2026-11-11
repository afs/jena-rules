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
import java.util.Objects;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;

/**
 * A {@link GroupProcessor} takes the stream of pattern matches (bindings)
 * splits them into groups, and passes the binds to the aggregation functions.
 * At he end it produce the output.
 */
public class GroupProcessor {
    // This is used as a distingished object meaning "no aggregates" marker. Use "==".
    private  final static Aggregator noAggregators = new AggregateNull();

    // A group key to the list of aggregators for that group.
    private final ListValuedMap<GroupKey, Aggregator> groups = new ArrayListValuedHashMap<>();
    private final List<Var> groupBy;
    private final GroupSplitter splitter;
    private final List<AggregatorFactory> aggregatorFactories;
    private List<Binding> rows;
    // The projection of the input row, projected to only group keys/
    private Binding currentGroupRow = null;

    public GroupProcessor(List<Var> groupBy, GroupSplitter groupSplitter, List<AggregatorFactory> aggregatorFactories) {
        // Null if this is GROUP with no BY.
        this.groupBy = groupBy;
        this.splitter = Objects.requireNonNull(groupSplitter);
        this.aggregatorFactories = Objects.requireNonNull(aggregatorFactories);
    }

    /** Called at the start of reduction */
    public void startReceive() {}

    /**
     * Reset for next row.
     * Having this, rather than a new object allows for caching in this GroupProcessor object.
     */
    public void nextRow(Binding row) {
        this.currentGroupRow = row;
        groups.clear();
    }

    public void receive(Binding binding) {
        // System.out.println("receive: "+binding);
        GroupKey groupKey = splitter.groupKey(binding);

        if ( groupKey == null )
            throw new InternalErrorException("Null group key: "+binding);

        // Maybe empty, never null.
        List<Aggregator> aggregators = groups.get(groupKey);
        if ( aggregators.isEmpty()) {
            // New aggregator.
            if ( aggregatorFactories.isEmpty() ) {
                // No aggregators but we need a key.
                groups.put(groupKey, noAggregators);
            } else {
                // New aggregators for this new group key value.
                for ( AggregatorFactory factory : aggregatorFactories ) {
                    Aggregator agg = factory.newAggregator(groupKey);
                    agg.start();
                    groups.put(groupKey, agg);
                    aggregators = groups.get(groupKey);
                }
            }
        }

        for ( Aggregator agg : aggregators  ) {
            if ( agg != null )
                agg.receive(binding);
        }
        //System.out.println("GroupProcessor: "+groupKey+" : "+binding);
    }

    public void finishReceive() {
        // System.out.println("finishReceive: "+currentGroupRow+" groups#="+groups.size());

        if ( ! groups.isEmpty() ) {
            rows = evalAgg(groups, currentGroupRow);
            return;
        }
        if ( groupBy == null || groupBy.isEmpty() ) {
            rows = evalAggEmptyGroups(currentGroupRow);
            return;
        }

        if ( aggregatorFactories.isEmpty() ) {
            rows = List.of(currentGroupRow);
            // Group keys.
        }
        else
            // GROUP BY, no grouping => no rows.
            rows = List.of();
    }

    // Empty grouping, no BY
    private List<Binding> evalAggEmptyGroups(Binding currentGroupRow) {
        BindingBuilder builder = BindingBuilder.create(currentGroupRow);
        // No aggregations?
        for ( AggregatorFactory aggMgr : aggregatorFactories ) {
            // May throw an ExprEvalException
            Node emptyValue = aggMgr.onEmpty();
            // KILL ROW
            if ( emptyValue == null )
                return List.of();
            builder.add(aggMgr.assignVar(), emptyValue);
        }
        Binding b = builder.build();
        return List.of(b);
    }

    // "parent" is the binding coming in from before the GROUP
    private List<Binding> evalAgg(ListValuedMap<GroupKey, Aggregator> groups, Binding parent) {
        // For each group, for each aggregator, calculate the result
        List<Binding> rows = new ArrayList<>();

        // System.out.println("Group keys: "+groups.keySet().size());

        // XXX Recode.
        for ( GroupKey gk : groups.keySet()) {
            BindingBuilder builder = (parent != null)
                    ? BindingBuilder.create(parent)
                    : BindingBuilder.create(BindingBuilder.noParent);
            // May or may not be present in the parent.
            // Ensure the group keys are set.
            gk.addToBinding(builder);
            boolean valid = true;
            for ( Aggregator agg : groups.get(gk) ) {
                if ( agg == noAggregators )
                    continue;
                agg.finish();
                Var v = agg.assignVar();
                Node n = agg.aggNode();
                // If no value then no row.
                if ( n == null ) {
                    valid = false;
                    continue;
                }
                builder.add(v,n);
            }
            if ( valid ) {
                Binding row = builder.build();
                rows.add(row);
            }
        }
        return rows;
    }

    public Iterator<Binding> eval() {
        return rows.iterator();
    }

    // This is used as a marker.
    private static class AggregateNull implements Aggregator {

        @Override public void start() {}
        @Override public void finish() {}
        @Override public void receive(Binding binding) {}
        @Override public Node aggNode() { throw new InternalErrorException("AggregateNull.aggNode"); }
        @Override public Var assignVar() { throw new InternalErrorException("AggregateNull.assignVar"); }
    }
}
