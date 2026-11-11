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
import org.seaborne.jena.srl.exec.RulesExecCxt;

public class Agg_Min implements AggregatorFactory {

    private final Var aggVar;
    private final Expr expr;
    private final RulesExecCxt rCxt;

    public Agg_Min(Var aggVar, Expr expr, RulesExecCxt rCxt) {
        this.aggVar = aggVar;
        this.expr = expr;
        this.rCxt = rCxt;
    }

    @Override
    public Aggregator newAggregator(GroupKey groupKey) {
        return new AggregateMin(aggVar, expr, rCxt);

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
        return null;
    }

    private static class AggregateMin implements Aggregator {
        private final Expr expr;
        private final RulesExecCxt rCxt;
        private final Var assignVar;
        private NodeValue min = null;

        private boolean valid = true;

        AggregateMin(Var assignVar, Expr expr, RulesExecCxt rCxt) {
            this.assignVar = assignVar;
            this.expr = expr;
            this.rCxt = rCxt;
        }

        @Override public void start() {}
        @Override public void finish() {}

        @Override
        public void receive(Binding binding) {
            try {
                // ExprEvalException (given the syntax and well-formedness, this
                // should not happen).
                NodeValue nv = expr.eval(binding, rCxt);
                if ( min == null )
                    min = nv;
                else {
                    // ExprEvalException
                    int cmp = NodeValue.compare(nv, min);
                    if ( cmp == Expr.CMP_LESS )
                        min = nv;
                }
            } catch (ExprEvalException ex) {
                // Invalid.
                valid = false;
                min = null;
            }
        }

        @Override
        public NodeValue aggValue() {
            if ( !valid )
                return null;
            return min;
        }

        @Override
        public Node aggNode() {
            if ( !valid )
                return null;
            return min.asNode();
        }

        @Override
        public Var assignVar() { return assignVar; }
    }
}
