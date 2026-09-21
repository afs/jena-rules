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

import java.util.List;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprNode;
import org.apache.jena.sparql.expr.ExprVisitor;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.graph.NodeTransform;
import org.seaborne.jena.srl.lang.RuleBodyElement;

public abstract class ExprReduce extends ExprNode {


    protected final List<RuleBodyElement> innerBody;

    protected ExprReduce(List<RuleBodyElement> innerBody) {
        this.innerBody = innerBody;
    }

    public List<RuleBodyElement> innerBody() {
        return innerBody;
    }

    public abstract Aggregator aggregator(Var resultVar);

    // ---- ExprNode
    @Override
    public void visit(ExprVisitor visitor) {}

    @Override
    public NodeValue eval(Binding binding, FunctionEnv env) {
        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Expr other, boolean bySyntax) {
        return false;
    }

    @Override
    public Expr copySubstitute(Binding binding) {
        return null;
    }

    @Override
    public Expr applyNodeTransform(NodeTransform transform) {
        return null;
    }
    // ---- ExprNode

}
