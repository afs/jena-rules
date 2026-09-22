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

import java.util.Iterator;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.graph.NodeConst;
import org.seaborne.jena.srl.lang.RuleBodyElement;


public class AggCount implements Aggregator {

    private final boolean distinct;
    private final Var aggVar;
    private final List<RuleBodyElement> innerBody;

    public AggCount(Var outputVar, boolean distinct, List<RuleBodyElement> innerBody) {
        //super(v, agg);
        this.aggVar = outputVar;
        this.innerBody = innerBody;
        this.distinct = distinct;
    }

    // Count(*)
    @SuppressWarnings("unused")
    @Override
    public Reducer reducer() {
        GroupKey gKey = new GroupKeyStar();
        return new Reducer("COUNT", distinct, aggVar,
                           ()->NodeConst.nodeZero,
                           _x->gKey,
                           rows->evalGroup(rows)
                );
    }

    private Node evalGroup(List<Binding> rows) {
        return NodeFactory.createLiteralDT(rows.size()+"", XSDDatatype.XSDinteger);
    }

    @Override
    public Iterator<Binding> eval(Reducer collector) {
        return collector.eval();
    }

    private static class GroupKeyStar extends GroupKey {
        // No key variables to add.
        @Override
        void addToBinding(BindingBuilder builder) {}
    }
}
