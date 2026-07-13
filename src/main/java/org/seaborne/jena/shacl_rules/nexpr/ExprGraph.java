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

package org.seaborne.jena.shacl_rules.nexpr;

import java.util.List;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.jena.JenaLib;
import org.seaborne.jena.shacl_rules.rdf_syntax.RVar;
import org.seaborne.jena.shacl_rules.sys.V;

/**
 * Encode/decode SPARQL expressions as RDF triples.
 */
public class ExprGraph {

    private static boolean insertSRLExpr = false;

    /**
     * Build a {@link Expr} starting from a given node in the graph. The node is used
     * as a subject and must have property {@code sh:expr}.
     *
     * @param graph
     * @param topNode Start of the expression.
     */
    public static Expr rdfToExpr(Graph graph, Node topNode) {
        // Check for [ srl:expr [ ...] ]
        Node exprNode = G.getZeroOrOneSP(graph, topNode, V.expr);
        if ( exprNode == null )
            exprNode = topNode;
        return buildExpr(graph, exprNode);
    }

    /**
     * Translate an {@link Expr} into RDF triples in the graph.
     * <p>
     * Return the blank node that encodes the function and
     * has the function URI as a predicate.
     */
    public static Node exprAsRDF(Graph graph, Expr expr) {
        Node x = buildRDF(graph, expr);
        if ( !insertSRLExpr )
            return x;
        Node x2 = NodeFactory.createBlankNode();
        graph.add(x2, V.expr, x);
        return x2;
    }

    // -------------

    private static Expr buildExpr(Graph graph, Node root) {
        // XXX [NX] In common with NodeExpressions.execNodeExpression??
        // Constant
        if ( ! root.isBlank() )
            return NodeValue.makeNode(root);

        // Variable?
        Var v = RVar.getVar(graph, root);
        if ( v != null )
            return new ExprVar(v);

        // General function.
        NodeExpressionFunction nExprFn = NX.getRDFExpression(graph, root);
        String functionURI = nExprFn.uri();
        List<Node> argsList = nExprFn.arguments();
        if ( argsList == null )
            throw new RuleExprEvalException("Bad node expression");

        // Registered in the node expression-only registry.
        Function function = NX.getFunction(functionURI);
        if ( function != null ) {
            //if ( NX.isRegistered(functionURI) ) {
            ExprList exprList = new ExprList();
            argsList.stream().map(n->buildExpr(graph, n)).forEach(exprList::add);
            return new E_Function(functionURI, exprList);
        }

        Expr[] args = argsList.stream().map(n->buildExpr(graph, n)).toArray(Expr[]::new);
        Expr expr = RuleExpr.build(functionURI, args);
        return expr;
    }

    // ---- Expr to RDF

    private static Node buildRDF(Graph graph, Expr expr) {

        switch(expr) {
            case NodeValue nv -> {
                // Constant.
                return nv.asNode();
            }
            case ExprVar nvar -> {
                // Blank node: [ srl:varName "varname" ] or [ shnex:var "varname" ]

                // Use Node expression form.
                return NX.addVar(graph, nvar.getVarName());
            }
            case ExprFunction exf -> {
                // [ function_uri (args) ]

                List<Expr> args = exf.getArgs();
                // Recursive step : arguments to RDF.
                List<Node> argNodes = args.stream().map(e->exprAsRDF(graph,e)).toList();

                Node argNodeList = JenaLib.createList(graph, argNodes);
                Node uri = RuleExpr.exprFunctionURI(exf, argNodes.size());
                Node x = NodeFactory.createBlankNode();
                graph.add(x, uri, argNodeList);
                return x;
            }
            case ExprAggregator agg -> {
                throw new ShaclException("Aggregation functions not supported");
            }
            case ExprTripleTerm x -> {
                throw new NotImplemented("exprToRDF:ExprTripleTerm");
            }
            case ExprNone x -> {
                throw new ShaclException("ExprNone not supported");
            }
            default -> {
                throw new ShaclException("Not recognized: "+expr.getClass());
            }
        }
    }
}
