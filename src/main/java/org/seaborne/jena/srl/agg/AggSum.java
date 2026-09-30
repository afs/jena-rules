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
import java.util.Iterator;
import java.util.List;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.XSDFuncOp;
import org.apache.jena.sparql.function.FunctionEnv;
import org.seaborne.jena.srl.exec.RulesExecCxt;
import org.seaborne.jena.srl.lang.RuleBodyElement;


public class AggSum implements Aggregator {

    private final boolean distinct;
    private final Var aggVar;
    private final Expr expr;
    private final List<RuleBodyElement> innerBody;
    private final RulesExecCxt rCxt;

    public AggSum(Var outputVar, boolean distinct, Expr expr, List<RuleBodyElement> innerBody, RulesExecCxt rCxt) {
        //super(v, agg);
        this.aggVar = outputVar;
        this.innerBody = innerBody;
        this.distinct = distinct;
        this.expr = expr;
        this.rCxt = rCxt;
    }

    @Override
    public GroupReducer reducer() {
        throw new NotImplemented();

//        GroupKey gKey = new GroupKeyStar();
//        return new Reducer("SUM", distinct, aggVar,
//                           ()->NodeConst.nodeZero,
//                           _x->gKey,
//                           rows->evalGroup(rows, rCxt)
//                );
    }

    private Node evalGroup(Collection<Binding> rows, RulesExecCxt rCxt) {
        NodeValue sum = NodeValue.nvZERO;
        FunctionEnv functionEnv = rCxt;
        for ( Binding row : rows ) {
            NodeValue nv = expr.eval(row, functionEnv);
            if ( ! nv.isNumber() )
                throw new ExprEvalException("SUM: Not a number");
            sum = XSDFuncOp.numAdd(sum, nv);
        }
        return sum.asNode();
    }

    @Override
    public Iterator<Binding> eval(GroupReducer reducer) {
        throw new NotImplemented();
//        return reducer.eval();
    }
}
