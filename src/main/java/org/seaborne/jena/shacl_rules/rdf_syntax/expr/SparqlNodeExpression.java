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

package org.seaborne.jena.shacl_rules.rdf_syntax.expr;

import java.util.List;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.sparql.util.graph.GNode;
import org.apache.jena.sparql.util.graph.GraphList;
import org.apache.jena.system.G;
import org.apache.jena.system.buffering.BufferingGraph;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.rdf_syntax.V;
import org.seaborne.jena.shacl_rules.rdf_syntax.expr.FunctionEverything.Build;

/**
 * Encode/decode SPAQRL expressions as RDF triples.
 */
public class SparqlNodeExpression {

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

            // Look for sh;expr
            Node expression1 = G.getZeroOrOneSP(graph, topNode, V.expr);
            if ( expression1 != null )
                return buildExpr(graph, expression1);

            // Look for sh:sparqlExpr
            Node expression2 = G.getZeroOrOneSP(graph, topNode, V.sparqlExpr);
            if ( expression2 != null)
                return buildSparqlExpr(graph, topNode);
            // Neither
            throw new ShaclException("sh:expr not found (nor sh:sparqlExpr)");
        } catch (Exception ex) {
            System.out.println("** failed to rebuild expr: "+ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Build an {@link Expr} from a node with a property {@code sh:expr}. The object
     * is a blank node structure of a SHACL Node Expression.
     * <p>
     * Prefer using {@link rdfToExpr} which switches between node expressions and
     * SPARQL expression strings.
     */
    public static Expr fromExpr(Graph graph, Node root) {
        return buildExpr(graph, root);
    }

    /**
     * Build an {@link Expr} from a node with a property {@code sh:sparqExpr}
     * whose value is a simple string in SPARQL expression syntax.
     * <p>
     * Prefer using {@link rdfToExpr} which switches between node expressions and
     * SPARQL expression strings.
s     */
    public static Expr fromSparqlExpr(Graph graph, Node root) {
        return buildSparqlExpr(graph, root);
    }

    /**
     * Encode an {@link Expr} into triples.
     * Return the node for the top of the expression.
     * The result node has a property {@code sh:expr}
     *
     *  or {@code sh:sparqlExpr}.
     * If it has both, the {@code sh:expr} is used to build the expression.
     */

    public static Node exprToRDF(Graph graph, Expr expr) {

        // XXX Remove later.
        BufferingGraph graphx = new BufferingGraph(graph);
        Node x = NodeFactory.createBlankNode();

        try {
            // [ sh:expr [ sparql:function (...) ] ]
            encodeAsExpr(graphx, x, expr);

        } catch (ShaclTranslateException ex) {
            graphx.reset();
            // Fallback.
            // XXX Remove later.
            encodeAsSparqlExpr(graphx, x, expr);
            graphx.flush();
            return x;
        }

        if ( IncludeSparqlExpr ) {
            // [ sh:exprSparql "..." ]
            encodeAsSparqlExpr(graphx, x, expr);
        }

        graphx.flush();
        return x;
    }

    private static Expr buildExpr(Graph graph, Node root) {
        if ( ! root.isBlank() )
            return NodeValue.makeNode(root);

        //private static Node extractVar(Graph graph, Node node) {
        Node vx = G.getZeroOrOneSP(graph, root, V.var);
        if ( vx != null ) {
            //Var v = Var.alloc(G.asString(vx));
            return new ExprVar(G.asString(vx));
        }
        //}

        Triple t = G.find(graph, root, null, null)
                // Ignores type,
                //.filterDrop(tt->tt.getPredicate().equals(RDF.Nodes.type))
                .next();

        Node pFunction = t.getPredicate();
        Node o = t.getObject();

        GNode gn = new GNode(graph, o);
        List<Node> list = GraphList.members(gn);

        // Replaces:
        List<Expr> args = list.stream().map(n->buildExpr(graph, n)).toList();

        String functionURI = pFunction.getURI();
        Build build = FunctionEverything.getBuild(pFunction.getURI());
        if ( build == null )
            throw new RuntimeException("Build: "+functionURI);

        // ???
        Expr[] array = args.toArray(Expr[]::new);
        Expr expr = build.build(functionURI, array);
        return expr;
    }

    private static Expr buildSparqlExpr(Graph graph, Node root) {
        Node exprSparqlNode = G.getZeroOrOneSP(graph, root, V.sparqlExpr);
        if ( exprSparqlNode == null )
            return null;
        if ( ! G.isString(exprSparqlNode) )
            throw new ShaclException("Not a simple string: "+exprSparqlNode);
        String exprString = G.asString(exprSparqlNode);
        return ExprUtils.parse(exprString);
    }

    // ---- Expr to RDF

    private static boolean IncludeSparqlExpr = false;

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
    private static Node exprAsRDF(Graph g, Expr expr) {
        switch(expr) {
            case NodeValue nv -> {
                // Constant.
                return nv.asNode();
            }
            case ExprVar nvar -> {
                // Blank node: [ sh:var "varname" ]
                Node x = NodeFactory.createBlankNode();
                Node str = NodeFactory.createLiteralString(nvar.getVarName());
                g.add(x, V.var, str);
                return x;
            }
            case ExprFunction exf -> {
                // [ function_uri (args) ]
                List<Expr> args = exf.getArgs();
                // Recursive step : arguments to RDF.
                List<Node> argNodes = args.stream().map(e->exprAsRDF(g,e)).toList();
                Node argNodeList = JLib.addList(g, argNodes);
                Node uri = exprFunctionURI(exf, argNodes.size());
                Node x = NodeFactory.createBlankNode();
                g.add(x, uri, argNodeList);
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
        String uri = FunctionEverything.getUriForExpr(exf);
        if ( uri == null )
            throw new ShaclTranslateException("Can't determine the URI for '"+exf.getFunctionPrintName(null)+"["+exf.getClass().getSimpleName()+"]' arity "+arity);
        return NodeFactory.createURI(uri);
    }
}
