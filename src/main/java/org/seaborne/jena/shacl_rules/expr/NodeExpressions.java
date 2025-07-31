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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import org.seaborne.jena.shacl_rules.expr.NodeExprTables.Call;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.rdf_syntax.V;

/**
 * SHACL node expression evaluation.
 * @see SparqlNodeExpressions for translation to/from SPARQL functions.
 */
public class NodeExpressions {
    static { INIT.init(); }

    /**
     * Node expression evaluation - this function evaluates starting from a root node
     * that is the subject of either {@code sh:expr} or {@code sh:sparqlExpr}.
     * <p>
     * See
     * {@link #evalNodeExpression(Graph, Node, Binding)} for execution of the node
     * expression itself.
     * </p>
     *
     * @implNote {@code sh:expr} is evaluated as a node expression tree in RDF, not
     *     by converting to a SPARQL expression.
     */

    public static NodeValue evaluate(Graph graph, Node root, Binding row) {
        Node x = getNodeExpression(graph, root);
        if ( x == null )
            // Warning?
            return null;
        return evalNodeExpression(graph, x, row);
    }

    /**
     * Execute a node expression in a node expression tree.
     * i.e. not {@code sh:expr} or {@code sh:sparqlExpr} - use {@link #evaluate(Graph, Node, Binding)} for those.
     */
    // List argument execution
    public static NodeValue evalNodeExpression(Graph graph, Node root, Binding row) {
        FunctionEnv functionEnv = new FunctionEnvBase(ARQ.getContext());
        return execNodeExpression(graph, root, row, functionEnv);
    }

    private static NodeValue execNodeExpression(Graph graph, Node root, Binding row, FunctionEnv functionEnv) {
        if ( ! root.isBlank() )
            return NodeValue.makeNode(root);

        if ( G.contains(graph, root, V.sparqlExpr, null) ) {
            // Should only happen if an expression can not be encoded in RDF.
            Expr expr = SparqlNodeExpressions.buildSparqlExpr(graph, root);
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

        // ---- Functional forms (= special cases)
        // Things that look like functions but process their argument in a special way.

        Triple functionTriple = NX.getFunctionTriple(graph, root);
        if ( functionTriple == null ) {}

        String uri = functionTriple.getPredicate().getURI();
        Node argsNode = functionTriple.getObject();
        // Return array.
        List<Node> args = JLib.asList(graph, argsNode);

        NodeExprTables.CallFF callFF = NodeExprTables.getCallFF(uri);
        if ( callFF != null ) {
            /* Functional forms look like functions - a URI and a list of arguments)
             * but aren't. Examples include {@code sh:if}, {@code sparql:coalesce},
             * {@code sparql:logical-and} which control the evaluation of their arguments,
             * and {@code sparql:bound} which tests a variables.
             */
            // sh:if is different. looks like a named function.
            // XXX for now, three argument sh:if
            Node[] a = args.toArray(Node[]::new);
            return callFF.execFF(graph, root, functionEnv, row, a);
        }

        // ---- General functions.
        // Evaluate arguments, call function.

        NodeValue[] evalArgs = args.stream().map(a->evalNodeExpression(graph, a, row)).toArray(NodeValue[]::new);
        NodeExprTables.Call call = NodeExprTables.getCall(uri);
        if ( call == null ) {
            // err
            throw new NodeExprEvalException("Failed to find a call: <"+uri+">");
        }
        return call.exec(evalArgs);
    }

    // --- Execution

    /**
     * Get a node expression - either {@code sh:expr} or {@code sh:sparqlExpr}.
     */
    /* package*/ static Node getNodeExpression(Graph graph, Node root) {
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
    /*package*/ static Node getListNodeExpression(Graph graph, Node root) {
        Node expressionNode = G.getZeroOrOneSP(graph, root, V.expr);
        return expressionNode;
    }

    // ---- Execution

    /**
     * Get a SPARQL node expression ({@code sh:sparqlExpr}).
     */
    /*package*/ static Node getSparqlExpression(Graph graph, Node root) {
        Node exprSparqlNode = G.getZeroOrOneSP(graph, root, V.sparqlExpr);
        if ( exprSparqlNode == null )
            return null;
        if ( ! G.isString(exprSparqlNode) )
            throw new ShaclException("Not a simple string: "+exprSparqlNode);
        return exprSparqlNode;
    }

    // XXX Temporary
    //sh:if is different ...
    static Set<Node> namedNodeExpressions = Set.of(V.ifCond);

    /** Execute a function node expression. */
    private static NodeValue exec(String uri, List<NodeValue>args) {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(args);
        NodeValue[] a = args.toArray(NodeValue[]::new);
        return exec(uri,a);
    }

    private static NodeValue exec(String uri, NodeValue...args) {
        Call call = NodeExprTables.getCall(uri);
        if ( call == null )
            throw new SPARQLEvalException("No such function: "+uri);
        return call.exec(args);
    }

    // ---- Integration into ARQ function execution

    static class INIT {
        static boolean initialized = false;
        // Called from a NodeExpressions class static which takes care of concurrency.
        static void init() {
            if ( ! initialized ) {
                initialized = true;
                init_loadFunctionRegistry(FunctionRegistry.get());
            }
        }

        // ---- Registration with ARQ

        /** Load the SPARQL functions into a {@link FunctionRegistry}. */
        private static void init_loadFunctionRegistry(FunctionRegistry reg) {
            Map<String, NodeExprTables.Call> map = NodeExprTables.mapDispatch();
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
}
