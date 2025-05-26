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
import java.util.Map;
import java.util.Objects;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.*;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.rdf_syntax.V;
import org.seaborne.jena.shacl_rules.rdf_syntax.expr.FunctionEverything.Call;

public class NodeExpressions {
    static { INIT.init(); }

    /**
     * Get a node expression - either {@code sh:expr}or {@code sh:sparqlExpr}.
     */
    public static Node getNodeExpression(Graph graph, Node root) {
        Node x1 = getListNodeExpression(graph, root);
        if ( x1 != null )
            return x1;
        Node x2 = getSparqlExpression(graph, root);
        if ( x2 != null )
            return x2;
        return null;
    }

    /**
     * Get a node expression ({@code sh:expr}), not a ({@code sh:sparqlExpr}).
     */
    public static Node getListNodeExpression(Graph graph, Node root) {
        Node expressionNode = G.getZeroOrOneSP(graph, root, V.expr);
        return expressionNode;
    }

    /**
     * Get a SPARQL node expression ({@code sh:sparqlExpr}).
     */
    public static Node getSparqlExpression(Graph graph, Node root) {
        Node exprSparqlNode = G.getZeroOrOneSP(graph, root, V.sparqlExpr);
        if ( exprSparqlNode == null )
            return null;
        if ( ! G.isString(exprSparqlNode) )
            throw new ShaclException("Not a simple string: "+exprSparqlNode);
        return exprSparqlNode;
    }


    // ---- Execution

    /**
     * Node expression evaluation - either {@code sh:expr} or {@code sh:sparqlExpr}.
     *
     * {@code sh:expr} is evaluated as a node expression tree in RDF,
     * not by converting to a SPARQL expression.
     */

    public static NodeValue execute(Graph graph, Node root, Binding row) {
        Node x = getNodeExpression(graph, root);
        if ( x == null )
            // Warning?
            return null;
        return execNodeExpression(graph, x, row);
    }

    private static String getFunctionURI(Graph graph, Node exprNode) {
        return G.getOne(graph, exprNode, null, null).getPredicate().getURI();
    }

    /**
     * Get the triple for a call.
     */
    private static Triple getFunctionTriple(Graph graph, Node exprNode) {
        //return G.getOneOrNull(graph, exprNode, null, null);
        //sh:if is different ...
        List<Triple> triples = G.find(graph, exprNode, null, null).toList();

        if ( triples.isEmpty() )
            return null;
        if ( triples.size() == 1 )
            return triples.getFirst();
        for ( Triple t : triples ) {
            if ( t.getPredicate().equals(V.ifCond) ) {
                return t;
            }
        }
        // Not if.
        System.out.println(triples.size());
        triples.forEach(System.out::println);
        throw new NodeExprEvalException("Multiple triples for function: "+triples);
    }

    /** Execute a function node expression. */
    private static NodeValue exec(String uri, List<NodeValue>args) {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(args);
        NodeValue[] a = args.toArray(NodeValue[]::new);
        return exec(uri,a);
    }

    private static NodeValue exec(String uri, NodeValue...args) {
        Call call = FunctionEverything.getCall(uri);
        if ( call == null )
            throw new SPARQLEvalException("No such function: "+uri);
        return call.exec(args);
    }

    static FunctionEnv functionEnv = new FunctionEnvBase(ARQ.getContext());

    /**
     * Execute a node expression in a node expression tree.
     * i.e. not {@code sh:expr} or {@code sh:sparqlExpr}
     */

    // XXX "execListArgument"
    public static NodeValue execNodeExpression(Graph graph, Node root, Binding row) {
        // XXX In common with SparqlNodeExpressions.buildExpr
        if ( ! root.isBlank() )
            return NodeValue.makeNode(root);

//        // Probe for sh:sparqlExpr as an expression.
//        Expr sparqLExpr = buildSparqlExpr(graph, callSite);
//        if ( sparqLExpr != null)
//            return sparqLExpr;

        if ( G.contains(graph, root, V.sparqlExpr, null) ) {
            // Should only happen is an expression ca no tbe encoded in RDF.
            Expr expr = SparqlNodeExpression.buildSparqlExpr(graph, root);
            return expr.eval(row, functionEnv);
        }

        // Variables?
        Node vx = G.getZeroOrOneSP(graph, root, V.var);
        if ( vx != null ) {
            Var v = Var.alloc(G.asString(vx));
            Node x = row.get(v);
            if ( x == null )
                throw new NodeExprEvalException("No value for variable "+v);
            return NodeValue.makeNode(x);
        }

        // General call form.

        Triple functionTriple = getFunctionTriple(graph, root);
        if ( functionTriple == null ) {}

        String uri = functionTriple.getPredicate().getURI();
        Node argsNode = functionTriple.getObject();
        // Return array.
        List<Node> args = JLib.asList(graph, argsNode);

        FunctionEverything.CallFF callFF = FunctionEverything.getCallFF(uri);
        if ( callFF != null ) {
            /* Functional forms look like functions - a URI and a list of arguments)
             * but aren't. Examples include {@code sh:if}, {@code sparql:coalesce},
             * {@code spatql:logical-and} which control the evaluation of their arguments,
             * and {@code sparql:bound} which tests a variables.
             */
            // sh:if is different. looks like a named function.
            // XXX for now, three argument sh:if

            Node[] a = args.toArray(Node[]::new);
            return callFF.execFF(graph, root, functionEnv, row, a);
        }

        // Function.

        NodeValue[] evalArgs = args.stream().map(a->execNodeExpression(graph, a, row)).toArray(NodeValue[]::new);
        FunctionEverything.Call call = FunctionEverything.getCall(uri);
        if ( call == null ) {
            // err
            throw new NodeExprEvalException("Failed to find a call: <"+uri+">");
        }
        return call.exec(evalArgs);
    }



    // ---- Integration into ARQ function execution

    static class INIT {
        static boolean initialized = false;
        static void init() {
            if ( ! initialized ) {
                initialized = true;
                loadFunctionRegistry(FunctionRegistry.get());
            }
        }

        // ---- Registration with ARQ

        /** Load the SPARQL functions into a {@link FunctionRegistry}. */
        private static void loadFunctionRegistry(FunctionRegistry reg) {
            Map<String, FunctionEverything.Call> map = FunctionEverything.mapDispatch();
            // Add to the system FunctionRegistry once.
            addToFunctionRegistry(FunctionRegistry.get(), map);
        }

        private static void addToFunctionRegistry(FunctionRegistry reg, Map<String, Call> map) {
            FunctionFactory ff = createFunctionFactory();
            map.forEach((uri,call) -> FunctionRegistry.get().put(uri, ff));
        }

        private static FunctionFactory createFunctionFactory() {
            return uri -> new FunctionBase() {
                @Override public NodeValue exec(List<NodeValue> args) { return NodeExpressions.exec(uri, args); }
                @Override public void checkBuild(String uri, ExprList args) {}
            };
        }
    }

    // ---- Execution




}
