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
import org.seaborne.jena.srl.exec.RulesEvalException;
import org.seaborne.jena.srl.exec.RulesExecCxt;

public class AggMin implements Aggregator {

    private final Collection<Var> groupBy;
    private final Var aggVar;
    private final Expr expr;
    private final RulesExecCxt rCxt;

    public AggMin(Var aggVar,  Collection<Var> groupBy, Expr expr, RulesExecCxt rCxt) {
        this.groupBy = groupBy;
        this.aggVar = aggVar;
        this.expr = expr;
        this.rCxt = rCxt;
    }

    @Override
    public GroupReducer reducer() {
        GroupSplitter splitter = GroupSplitters.splitter(groupBy);
        return new GroupReducer("MIN", aggVar,
                                ()->{ throw new RulesEvalException("Empty group for MAX"); },
                                splitter,
                                k->new AggregateMax(expr, rCxt)
                );
    }

    private static class AggregateMax extends AggregateFunction {
        private final Expr expr;
        private final RulesExecCxt rCxt;
        private NodeValue min = null;

        AggregateMax(Expr expr, RulesExecCxt rCxt) {
            this.expr = expr;
            this.rCxt = rCxt;
        }
        @Override
        public void receive(Binding binding) {
            NodeValue nv = expr.eval(binding, rCxt);
            if ( min == null )
                min = nv;
            else {
                int cmp = NodeValue.compare(nv, min);
                if ( cmp == Expr.CMP_LESS )
                    min = nv;
            }
        }

        @Override
        public NodeValue aggValue() { return min; }

        @Override
        public Node aggNode() { return min.asNode(); }
    }
}
