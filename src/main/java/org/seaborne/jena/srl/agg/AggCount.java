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

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.graph.NodeConst;
import org.seaborne.jena.srl.exec.RulesExecCxt;
import org.seaborne.jena.srl.lang.RuleBodyElement;


public class AggCount implements Aggregator {

    // Abstract super class?

    private final boolean distinct;
    private final Var aggVar;
    private final List<RuleBodyElement> innerBody;
    private final RulesExecCxt rCxt;
    private final Collection<Var> groupBy;

    public AggCount(Var outputVar, boolean distinct, Collection<Var> groupBy, List<RuleBodyElement> innerBody, RulesExecCxt rCxt) {
        //super(v, agg);
        this.aggVar = outputVar;
        this.innerBody = innerBody;
        this.distinct = distinct;
        this.groupBy = groupBy;
        this.rCxt = rCxt;
    }

    // Count(*)
    @Override
    public Reducer reducer() {
        GroupSplitter groupSplitter;

//        protected Reducer(String name, boolean isDistinct, Var outputVar,
//                          Supplier<Node> onEmpty,
//                          Function<Binding, GroupKey> groupSplitter,
//                          Function<Collection<Binding>, Node> groupValue) {

        // Do we need an order group set?

        if ( groupBy.isEmpty() ) {
            groupSplitter =  new GroupSplitter() {
                @Override
                public GroupKey groupKey(Binding binding) { return GroupKeyFixed.instance; }

                @Override
                public Set<Var> groupKeys() { return Set.of(); }
            };
        } else {
            // Unique (per reducer) order
            List<Var> keyVars = List.copyOf(groupBy);
            groupSplitter = new GroupSplitter() {
                @Override
                public GroupKey groupKey(Binding binding) {
                    List<Node> key = new ArrayList<>(groupBy.size());
                    for ( Var var : keyVars ) {
                        Node n = binding.get(var);
                        if ( n == null ) {};
                        key.add(n);
                    }
                    return new GroupKeyList(keyVars, key);
                }
                @Override
                public Collection<Var> groupKeys() {
                    return groupBy;
                }
            };
        }
        return new Reducer("COUNT", distinct, aggVar,
                           ()->NodeConst.nodeZero,
                           /*splitter*/    groupSplitter,
                           /*gourpValue*/  rows->evalGroup(rows, rCxt)
                );
    }

    private Node evalGroup(Collection<Binding> rows, RulesExecCxt rCxt) {
        return NodeFactory.createLiteralDT(rows.size()+"", XSDDatatype.XSDinteger);
    }

    @Override
    public Iterator<Binding> eval(Reducer reducer) {
        return reducer.eval();
    }
}
