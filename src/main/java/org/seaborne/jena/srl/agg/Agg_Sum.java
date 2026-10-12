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

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.XSDFuncOp;
import org.apache.jena.sparql.graph.NodeConst;
import org.seaborne.jena.srl.exec.RulesExecCxt;


public class Agg_Sum implements AggregatorFactory {

    private final Var aggVar;
    private final Expr expr;
    private final RulesExecCxt rCxt;

    public Agg_Sum(Var assignVar, Expr expr, RulesExecCxt rCxt) {
        this.aggVar = assignVar;
        this.expr = expr;
        this.rCxt = rCxt;
    }

    @Override
    public Aggregator newAggregator(GroupKey groupKey) {
        return new AggregateSum(aggVar, expr, rCxt);
    }

    @Override
    public Expr getArgument() {
        return expr;
    }

    @Override
    public Var assignVar() {
        return aggVar;
    }

    @Override
    public Node onEmpty() {
        // XXX ?
        //throw new ExprEvalException("sum of empty");
        return NodeConst.nodeZero;
    }

    private static class AggregateSum implements Aggregator {
        private final Expr expr;
        private final RulesExecCxt rCxt;
        private final Var assignVar;
        private NodeValue sum = null;

        AggregateSum(Var assignVar, Expr expr, RulesExecCxt rCxt) {
            this.assignVar = assignVar;
            this.expr = expr;
            this.rCxt = rCxt;
        }

        @Override
        public void receive(Binding binding) {
            NodeValue nv = expr.eval(binding, rCxt);
            if ( sum == null )
                sum = nv;
            else
                sum = XSDFuncOp.numAdd(sum, nv);
        }

        @Override
        public NodeValue aggValue() {
            if ( sum == null )
                throw new ExprEvalException("sum of empty");
            return sum;
        }

        @Override
        public Node aggNode() { return sum.asNode(); }

        @Override
        public Var assignVar() { return assignVar; }
    }

}
