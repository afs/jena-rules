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

package org.seaborne.jena.shacl_rules.expr;

import java.util.List;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.system.G;
import org.apache.jena.system.buffering.BufferingGraph;
import org.seaborne.jena.shacl_rules.expr.NodeExprTables.Build;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.lang.ExprNodeExpression;
import org.seaborne.jena.shacl_rules.rdf_syntax.V;

/**
 * Encode/decode SPAQRL expressions as RDF triples.
 */
public class SparqlNodeExpressions {

    /**
     * Build a {@link Expr} starting from a given node in the graph.
     * The node is used as a subject and must have property {@code sh:expr} or {@code sh:sparqlExpr}.
     * If it has both, the {@code sh:expr} is used to build the expression.
     *
     * @param graph
     * @param topNode Start of the expression.
     */
    public static Expr rdfToExpr(Graph graph, Node topNode) {
        try {
            // [] sh:expr ...
            //   or
            // [] sh:sparqlExpr ...

            // Duplicate sh:expr?
            // Look for sh:expr, return object
            Node expression1 = NodeExpressions.getNodeExpression(graph, topNode);
            if ( expression1 != null )
                return buildExpr(graph, expression1);

            // Look for sh:sparqlExpr, return object (which is a string).
            Node expression2 = NodeExpressions.getSparqlExpression(graph, topNode);
            if ( expression2 != null)
                return buildSparqlExpr(graph, topNode);
            // Neither
            throw new ShaclException("sh:expr not found (nor sh:sparqlExpr)");
        } catch (Exception ex) {
//            System.out.println("** failed to rebuild expr: "+ex.getMessage());
//            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Build an {@link Expr} from a node with a property {@code sh:expr}. The object
     * is a blank node structure of a SHACL Node Expression.
     * <p>
     * Prefer using {@link #rdfToExpr} which switches between node expressions and
     * SPARQL expression strings.
     */
    public static Expr fromExpr(Graph graph, Node root) {
        return buildExpr(graph, root);
    }

    /**
     * Build an {@link Expr} from a node with a property {@code sh:sparqExpr}
     * whose value is a simple string in SPARQL expression syntax.
     * <p>
     * Prefer using {@link #rdfToExpr} which switches between node expressions and
     * SPARQL expression strings.
     */
    public static Expr fromSparqlExpr(Graph graph, Node root) {
        return buildSparqlExpr(graph, root);
    }

    /**
     * Encode an {@link Expr} into triples.
     * Return the node for the top of the expression.
     * The result node has a property {@code sh:expr}
     * or {@code sh:sparqlExpr} if the translation fails.
     */

    public static Node exprToRDF(Graph graph, Expr expr) {
        return exprToRDF(graph, expr, false);
    }

    /**
     * Encode an {@link Expr} into triples.
     * Return the node for the top of the expression.
     * The result node has a property {@code sh:expr}
     * and also always has {@code sh:sparqlExpr}.
     */
    public static Node exprToRDF(Graph graph, Expr expr, boolean includeSparqlExpr) {
        // XXX Remove later.
        BufferingGraph graphx = new BufferingGraph(graph);
        Node x = NodeFactory.createBlankNode();

        try {
            // [ sh:expr [ sparql:function (...) ] ]
            encodeAsExpr(graphx, x, expr);
        } catch (ShaclTranslateException ex) {
            graphx.reset();
            // Fallback.
            // XXX Remove later?
            encodeAsSparqlExpr(graphx, x, expr);
            graphx.flush();
            return x;
        }

        if ( includeSparqlExpr ) {
            // [ sh:exprSparql "..." ]
            encodeAsSparqlExpr(graphx, x, expr);
        }
        graphx.flush();
        return x;
    }

    private static final boolean UseExprNodeExpression = true;

    /*package*/static Expr buildExpr(Graph graph, Node root) {

        if ( UseExprNodeExpression )
            return ExprNodeExpression.create(graph, root);

        // In common with NodeExpressions.execNodeExpression
        // Constant
        if ( ! root.isBlank() )
            return NodeValue.makeNode(root);

        // root sh:sparqlExpr ...
        if ( G.contains(graph, root, V.sparqlExpr, null) ) {
            return buildSparqlExpr(graph, root);
        }

        // Variable?
        Var v = NX.getVar(graph, root);
        if ( v != null )
            return new ExprVar(v);

        // General function.
        NodeExpressionFunction nExprFn = NX.getRDFExpression(graph, root);
        String functionURI = nExprFn.uri();
        List<Node> list = nExprFn.arguments();
        // Convert arguments to Expr.
        List<Expr> args = list.stream().map(n->buildExpr(graph, n)).toList();
        Build build = NodeExprTables.getBuild(functionURI);
        if ( build == null )
            throw new RuntimeException("Build: "+functionURI);
        // XXX List/Array
        Expr[] array = args.toArray(Expr[]::new);
        Expr expr = build.build(functionURI, array);
        return expr;
    }

    /*package*/ public static Expr buildSparqlExpr(Graph graph, Node root) {
        Node exprSparqlNode = NodeExpressions.getSparqlExpression(graph, root);
        if ( exprSparqlNode == null )
            return null;
        String exprString = G.asString(exprSparqlNode);
        return ExprUtils.parse(exprString);
    }

    // ---- Expr to RDF

    /**
     * Add {@code sh:expr node expression} (RDF syntax node expression)
     * for a SHACL rules expression.
     * No {@code rdf:type} is added.
     */
    private static void encodeAsExpr(Graph graph, Node x, Expr expr) {
        Node exprNode = exprAsRDF(graph, expr);
        graph.add(x, V.expr, exprNode);
    }

    /**
     * Add {@code sh:sparqlExpr "expression string"} (a SPARQL syntax string).
     * No {@code rdf:type} is added.
     */
    private static void encodeAsSparqlExpr(Graph graph, Node x, Expr expr) {
        graph.add(x, V.sparqlExpr, exprAsString(expr));
    }

    /** Expression to SPARQL syntax string */
    private static Node exprAsString(Expr expr) {
        IndentedLineBuffer out = new IndentedLineBuffer();
        ExprUtils.fmtSPARQL(out, expr);
        //WriterSSE.out(out, expr, null);
        return NodeFactory.createLiteralString(out.asString());
    }

    /**
     * Encode an {@link Expr} as RDF triples in the graph.
     * <p>
     * Return the blank node that encodes the function and
     * has the function URI as a predicate.
     */
    private static Node exprAsRDF(Graph graph, Expr expr) {
        switch(expr) {
            case NodeValue nv -> {
                // Constant.
                return nv.asNode();
            }
            case ExprVar nvar -> {
                // Blank node: [ sh:var "varname" ]
                return NX.addVar(graph, nvar.getVarName());
            }
            case ExprFunction exf -> {
                // [ function_uri (args) ]
                List<Expr> args = exf.getArgs();
                // Recursive step : arguments to RDF.
                List<Node> argNodes = args.stream().map(e->exprAsRDF(graph,e)).toList();
                Node argNodeList = JLib.addList(graph, argNodes);
                Node uri = exprFunctionURI(exf, argNodes.size());
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

    /** For a given SPARQL function or functional form, encode in RDF. */
    private static Node exprFunctionURI(ExprFunction exf, int arity) {
        String uri = NodeExprTables.getUriForExpr(exf);
        if ( uri != null )
            return NodeFactory.createURI(uri);
        // No lookup. Either pass out anyway or signal an error.
        if ( false )
            // No custom URI functions.
            throw new ShaclTranslateException("Can't determine the URI for '"+exf.getFunctionPrintName(null)+"["+exf.getClass().getSimpleName()+"]' arity "+arity);
        return NodeFactory.createURI(exf.getFunctionIRI());
    }
}
