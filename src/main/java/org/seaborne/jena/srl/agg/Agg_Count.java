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

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.graph.NodeConst;
import org.seaborne.jena.srl.exec.RulesExecCxt;


/**
 * COUNT, COUNT DISTINCT
 */
public class Agg_Count implements AggregatorFactory {
    private final boolean distinct;
    private final RulesExecCxt rCxt;
    private final Var assignVar;
    private final Expr expr;

    public Agg_Count(boolean distinct, Var assignVar, Expr expr, RulesExecCxt rCxt) {
        this.distinct = distinct;
        this.assignVar = assignVar;
        this.expr = expr;
        this.rCxt = rCxt;
    }


    @Override
    public Expr getArgument() {
        return expr;
    }

    @Override
    public Node onEmpty() {
        return NodeConst.nodeZero;
    }

    @Override
    public Var assignVar() {
        return assignVar;
    }

    @Override
    public Aggregator newAggregator(GroupKey groupKey) {
        return new AggregateCount(distinct, assignVar, expr);
        }

    private static class AggregateCount implements Aggregator {

        private final boolean distinct;
        private final Var assignVar;
        private final Expr expr;
        private final Set<NodeValue> seen;

        AggregateCount(boolean distinct, Var assignVar, Expr expr) {
            this.distinct = distinct;
            this.assignVar = assignVar;
            this.expr = expr;
            this.seen = (expr!=null) ? new HashSet<>() : null;
        }

        private long counter = 0 ;
        @Override
        public void receive(Binding binding) {
            if ( expr == null ) {
                counter++;
                return;
            }

            // XXX
            NodeValue z = expr.eval(binding, null);
            if ( seen.contains(z) )
                return;
            seen.add(z);
            counter++;
        }

        @Override
        public NodeValue aggValue() { return NodeValue.makeInteger(counter); }

        @Override
        public Node aggNode() { return NodeFactory.createLiteralDT(Long.toString(counter), XSDDatatype.XSDinteger); }

        @Override
        public Var assignVar() {
            return assignVar;
        }
    }
}
