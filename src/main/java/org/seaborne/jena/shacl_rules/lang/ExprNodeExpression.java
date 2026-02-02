/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.seaborne.jena.shacl_rules.expr.NX;
import org.seaborne.jena.shacl_rules.expr.NodeExpressionFunction;
//import org.seaborne.jena.shacl_rules.expr.NodeExpressionFunction;
import org.seaborne.jena.shacl_rules.expr.NodeExpressions;
import org.seaborne.jena.shacl_rules.expr.SparqlNodeExpressions;
import org.seaborne.jena.shacl_rules.rdf_syntax.RVar;

/**
 * An {@link Expr} wrapper for a NodeExpression.
 */
public class ExprNodeExpression extends ExprNode {

    private final Node exprNode;
    private final Graph graph;
    private Expr asExpr = null;
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

    private Expr asExpr() {
        if ( asExpr == null ) {
            // Delayed calculation.
            synchronized(ExprNodeExpression.class) {
                if ( asExpr != null )
                    return asExpr;
                asExpr = convertToExpr();
            }
        }
        return asExpr;
    }

    // Conversion. This is expected to work.
    private Expr convertToExpr() {
        try {
            return SparqlNodeExpressions.buildSparqlExpr(graph, exprNode);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Return a {@link Expr} that is this node expression,
     * converted to a SPARQL function structure internally.
     */
    public Expr converted() {
        return asExpr();
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

        // [ sh:sparqlExpr "..." ]
        // Should only happen if an expression can not be encoded in RDF.
        Expr expr = SparqlNodeExpressions.fromSparqlExpr(graph, node);
        if ( expr != null ) {
            vars.addAll(expr.getVarsMentioned());
            return;
        }

        // Accepts either predicate for a variable.
        Var var = RVar.getVar(graph, node);
        if ( var != null ) {
            // Variable.
             vars.add(var);
             return;
         }

        // It's a function
        //  [ function ( args1, args2) ]
        // or shl:expr.

        NodeExpressionFunction nExprFn = NX.getRDFExpression(graph, node);
        for ( Node arg : nExprFn.arguments() ) {
            accVars(vars, graph, arg);
        }
    }

    @Override
    public void visit(ExprVisitor visitor) {
        if ( asExpr == null)
            return;
        // Variable extraction is done in the constructor.
        Expr x1 = SparqlNodeExpressions.rdfToExpr(graph, exprNode);
        x1.visit(visitor);
    }

    @Override
    public NodeValue eval(Binding binding, FunctionEnv env) {
        return NodeExpressions.evalNodeExpression(graph, exprNode, binding);
    }

    @Override
    public Expr copySubstitute(Binding binding) {
        Expr x1 = SparqlNodeExpressions.rdfToExpr(graph, exprNode);
        Expr x2 = x1.copySubstitute(binding);
        return x2;
    }

    @Override
    public Expr applyNodeTransform(NodeTransform transform) {
        Expr x1 = SparqlNodeExpressions.rdfToExpr(graph, exprNode);
        Expr x2 = x1.applyNodeTransform(transform);
        return x2;
    }

    @Override
    public int hashCode() {
        if ( asExpr != null )
            return asExpr.hashCode();
        return exprNode.hashCode();
    }

    @Override
    public boolean equals(Expr other, boolean bySyntax) {
        if ( asExpr != null )
            return asExpr.equals(other);
        if ( ! ( other instanceof ExprNodeExpression exprNX ) )
            return false;
        // This rather strong but asExpr didn't work.
        return
                graph == exprNX.graph &&
                exprNode.equals(exprNX.exprNode);
    }
}