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

package org.seaborne.jena.shacl_rules.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.graph.NodeTransform;
import org.seaborne.jena.shacl_rules.nexpr.NX;
import org.seaborne.jena.shacl_rules.nexpr.NodeExpressionFunction;
import org.seaborne.jena.shacl_rules.nexpr.NodeExprEval;
import org.seaborne.jena.shacl_rules.nexpr.ExprGraph;
import org.seaborne.jena.shacl_rules.rdf_syntax.RVar;

/**
 * An {@link Expr} wrapper for a NodeExpression.
 */
public class ExprNodeExpression extends ExprNode {

    private final Node exprNode;
    private final Graph graph;
    private final Set<Var> varsMentioned;

    public static Expr create(Graph graph, Node node) {
        ExprNodeExpression exprNodeExpr = new ExprNodeExpression(graph, node);
        return exprNodeExpr;
    }

    private ExprNodeExpression(Graph graph, Node node) {
        this.exprNode = node;
        this.graph = graph;
        // Calculate the vars based on the node expression.
        List<Var> vars = new ArrayList<>();
        accVars(vars, graph, exprNode);
        varsMentioned = Set.of(vars.toArray(Var[]::new));
    }

    private static String exprURI(Node node) {
        try {
            return node.getURI();
        } catch (RuntimeException ex) {
            throw new ExprEvalException("Node expression name is not a URI: "+NodeFmtLib.displayStr(node));
        }
    }

    @Override
    public final Set<Var> getVarsMentioned() {
        return varsMentioned;
    }

    /** Find and record variables. */
    private static void accVars(Collection<Var> vars, Graph graph, Node node) {
        if (! node.isBlank() )
            // Constant.
            return;

        // Accepts either predicate for a variable.
        Var var = RVar.getVar(graph, node);
        if ( var != null ) {
            // Variable.
             vars.add(var);
             return;
         }

        // It's a function
        //  [ function ( args1, args2) ]
        // or srl:expr.

        NodeExpressionFunction nExprFn = NX.getRDFExpression(graph, node);
        for ( Node arg : nExprFn.arguments() ) {
            accVars(vars, graph, arg);
        }
    }

    @Override
    public void visit(ExprVisitor visitor) {
        // Variable extraction is done in the constructor.
        Expr x1 = ExprGraph.rdfToExpr(graph, exprNode);
        x1.visit(visitor);
    }

    @Override
    public NodeValue eval(Binding binding, FunctionEnv env) {
        return NodeExprEval.evalNodeExpression(graph, exprNode, binding, env);
    }

    @Override
    public Expr copySubstitute(Binding binding) {
        Expr x1 = ExprGraph.rdfToExpr(graph, exprNode);
        Expr x2 = x1.copySubstitute(binding);
        return x2;
    }

    @Override
    public Expr applyNodeTransform(NodeTransform transform) {
        Expr x1 = ExprGraph.rdfToExpr(graph, exprNode);
        Expr x2 = x1.applyNodeTransform(transform);
        return x2;
    }

    @Override
    public int hashCode() {
        return exprNode.hashCode();
    }

    @Override
    public boolean equals(Expr other, boolean bySyntax) {
        if ( ! ( other instanceof ExprNodeExpression exprNX ) )
            return false;
        // This rather strong but asExpr didn't work.
        return
                graph == exprNX.graph &&
                exprNode.equals(exprNX.exprNode);
    }
}