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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.urifunctions.SPARQLFuncOp;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.system.G;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.rdf_syntax.V;

/**
 * Encode/decode SPAQRl expressions as RDF triples.
 */
public class SparqlNodeExpression {

    /**
     * Encode an {@link Expr} into triples.
     * Return the node for the top of the expression.
     */
    public static Node sparqlExprToRDF(Graph graph, Expr expr) {
        Node exprNode = exprAsRDF(graph, expr);
        Node x = NodeFactory.createBlankNode();
        // sh:expr
        graph.add(x, V.expr, exprNode);
        // Also sh:exprSparql
        graph.add(x, V.sparqlExpr, exprAsString(expr));
        graph.add(x, RDF.Nodes.type, V.sparqlExprClass);
        return x;
    }

    private static Node exprAsString(Expr expr) {
        IndentedLineBuffer out = new IndentedLineBuffer();
        ExprUtils.fmtSPARQL(out, expr);
        //WriterSSE.out(out, expr, null);
        return NodeFactory.createLiteralString(out.asString());
    }

    public static Expr rdfToSparqlExpr(Graph graph, Node node) {
        if ( ! node.isBlank() )
            // Constant.
            return NodeValue.makeNode(node);

        List<Triple> z = G.find(graph, node, null, null).toList();    // Predicate is the function name.
        if ( z.isEmpty() )
            throw new ShaclException("Failed to find an expression");
        if ( z.size() > 1 )
            throw new ShaclException("Expression node has more than one triple");
        Triple t = z.getFirst();
        Node fn = t.getPredicate();

        if ( fn.equals(V.var) ) {
            Node vn = t.getObject();
            if ( ! G.isString(vn) )
                throw new ShaclException("Variable name is not a simple string: "+vn);
            String varName = G.asString(vn);
            return new ExprVar(varName);
        }

        List<Node> argNodes = JLib.asList(graph, t.getObject());
        // Recursive step.
        List<Expr> args = argNodes.stream().map(n-> rdfToSparqlExpr(graph, n)).toList();
        ExprList argsExprList = new ExprList(args);

        // TODO Generate as a <uri>() function call.
        // **** Do better!
        Expr expr = new E_Function(fn.getURI(), argsExprList);
        return expr;
    }

    /**
     * Find all the SPARQL expressions encoded in RDF in the graph.
     * This is done by looking to object of {@code sh:expr}.
     */
    public static List<Expr> rdfToSparqlExpr(Graph graph) {
        List<Node> expressionNodes = Iter.toList(G.iterObjectsOfPredicate(graph, V.expr));
        List<Expr> expressions = new ArrayList<>();
        expressionNodes.forEach(expressionNode->{
            Node n = expressionNode; //G.getOneSP(graph, expressionNode, V.expr);
            Expr expr = rdfToSparqlExpr(graph, n);
            expressions.add(expr);
        });
        return expressions;
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

    // XXX Replace me!
    private static Node exprFunctionURI(ExprFunction exf, int arity) {
        String uri = FunctionEverything.uriForExpr(exf);
        if ( uri == null )
            throw new ShaclException("Can't determine the URI for '"+exf.getFunctionPrintName(null)+"["+exf.getClass().getSimpleName()+"]' arity "+arity);
        return NodeFactory.createURI(uri);

//        Class<?> cls = exf.getClass();
//        return classToURI.get(cls);
    }

    // ---- Development

    // 1 - add arity to SPARQL Dispatch table
    // 2 - dispatch/builder pattern [and not 1?]

    // A few for development.
    static String NS = SPARQLFuncOp.NS;
    static Map<Class<?>, Node> classToURI = buildClassToURI();
    static Map<Class<?>, Node> buildClassToURI() {
        Map<Class<?>, Node> map = new HashMap<>();
        register(map, E_Add.class, "plus");
        register(map, E_Subtract.class, "subtract");
        register(map, E_Multiply.class, "multiply");
        register(map, E_GreaterThan.class, "greaterThan");
        register(map, E_LessThan.class, "lessThan");
        register(map, E_GreaterThanOrEqual.class, "greaterThanOrEqual");
        register(map, E_LessThanOrEqual.class, "greaterThanOrEqual");
        return Map.copyOf(map);
    }

    private static void register(Map<Class<?>, Node> map, Class<?> klass, String localName) {
        if ( localName.contains(":") )
            // Internal check.
            throw new JenaException("Not a local name");
        Node uri = NodeFactory.createURI(NS+localName);
        map.put(klass, uri);
    }
}
