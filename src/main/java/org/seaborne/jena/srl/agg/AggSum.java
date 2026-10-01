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

import java.util.Collection;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.XSDFuncOp;
import org.apache.jena.sparql.graph.NodeConst;
import org.seaborne.jena.srl.exec.RulesExecCxt;


public class AggSum implements Aggregator {

    private final Var aggVar;
    private final Expr expr;
    private final Collection<Var> groupBy;
    private final RulesExecCxt rCxt;

    public AggSum(Var outputVar, Collection<Var> groupBy, Expr expr, RulesExecCxt rCxt) {
        this.aggVar = outputVar;
        this.expr = expr;
        this.groupBy = groupBy;
        this.rCxt = rCxt;
    }

    @Override
    public GroupReducer reducer() {
        GroupSplitter groupSplitter = (groupBy.isEmpty()) ? GroupSplitters.splitterStar() : GroupSplitters.splitterVars(groupBy);
        return new GroupReducer("SUM", aggVar,
                                ()->NodeConst.nodeZero,
                                groupSplitter,
                                k->new AggregateSum(expr, rCxt)
                               );
    }

    private static class AggregateSum extends AggregateFunction {

        private final Expr expr;
        private final RulesExecCxt rCxt;

        AggregateSum(Expr expr, RulesExecCxt rCxt) {
            this.expr = expr;
            this.rCxt = rCxt;
        }

        private NodeValue sum = NodeValue.makeInteger(0L);

        @Override
        public void receive(Binding binding) {
            NodeValue nv = expr.eval(binding, rCxt);
            sum = XSDFuncOp.numAdd(sum, nv);
        }

        @Override
        public NodeValue aggValue() { return sum; }

        @Override
        public Node aggNode() { return sum.asNode(); }
    }
}
