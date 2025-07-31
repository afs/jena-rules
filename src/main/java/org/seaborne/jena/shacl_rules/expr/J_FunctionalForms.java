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

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.XSDFuncOp;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.rdf_syntax.V;

/**
 * Functional forms.
 */
public class J_FunctionalForms {

    static NodeValue sparql_logical_and(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, Node arg1, Node arg2) {
        Expr expr1 = SparqlNodeExpressions.buildExpr(graph, arg1);
        Expr expr2 = SparqlNodeExpressions.buildExpr(graph, arg2);
        Expr x = new E_LogicalAnd(expr1, expr2);
        return x.eval(row, functionEnv);
    }

    static NodeValue sparql_logical_or(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, Node arg1, Node arg2) {
        Expr expr1 = SparqlNodeExpressions.rdfToExpr(graph, arg1);
        Expr expr2 = SparqlNodeExpressions.rdfToExpr(graph, arg2);
        Expr x = new E_LogicalOr(expr1, expr2);
        return x.eval(row, functionEnv);
    }

    static NodeValue sparql_logical_not(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, Node arg1) {
        Expr expr1 = SparqlNodeExpressions.rdfToExpr(graph, arg1);
        Expr x = new E_LogicalNot(expr1);
        return x.eval(row, functionEnv);
    }

    static NodeValue shacl_if(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, Node condition) {
        Node thenArg = G.getSP(graph, callNode, V.ifThen);
        Node elseArg = G.getSP(graph, callNode, V.ifElse);
        return shacl_if(graph, callNode, functionEnv, row, condition, thenArg, elseArg);
    }

    // Pure form. [ sh:if ( condition then else ) ]
    static NodeValue shacl_if(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, Node condition, Node thenArg, Node elseArg) {
        return sparql_if(graph, callNode, functionEnv, row, condition, thenArg, elseArg);
    }

    // SPARQL IF(condition, then, else)
    static NodeValue sparql_if(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, Node condition, Node thenArg, Node elseArg) {
        NodeValue nv = NodeExpressions.evalNodeExpression(graph, condition, row);
        boolean b = XSDFuncOp.effectiveBooleanValue(nv);
        if ( b ) {
            if ( thenArg != null )
                return NodeExpressions.evalNodeExpression(graph, thenArg, row);
        } else {
            if ( elseArg != null )
                return NodeExpressions.evalNodeExpression(graph, elseArg, row);
        }
        return XSDFuncOp.effectiveBooleanValueAsNodeValue(nv);

    }

    // Arity N
    // [ sparql:coalesce ( arg1 arg2 ...) ]
    static NodeValue sparql_coalesce(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, List<Node> args) {
        for ( Node arg : args ) {
            try {
                NodeValue nv = NodeExpressions.evalNodeExpression(graph, arg, row);
                if ( nv == null )
                    throw new InternalErrorException("Node expressiom return null");
                return nv;
            } catch (ExprEvalException | NodeExprEvalException ex) { }
        }
        throw new NodeExprEvalException("COALESCE: no value") ;
    }

    // [ sparql:in ( valueExpr arg1 arg2 ...) ]
    static NodeValue sparql_in(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, List<Node> args) {
        boolean b = sparql_oneof(graph, callNode, functionEnv, row, args);
        return NodeValue.booleanReturn(b);
    }

    static NodeValue sparql_not_in(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, List<Node> args) {
        boolean b = sparql_oneof(graph, callNode, functionEnv, row, args);
        return NodeValue.booleanReturn(!b);
    }

    // Worker function.
    private static boolean sparql_oneof(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, List<Node> args) {
        if ( args.isEmpty() )
            throw new NodeExprEvalException("argument list is empty");

        // Alt - build the Expr and make a E_OneOf to eval.

        Node valueNode = args.getFirst();
        NodeValue value = NodeExpressions.evalNodeExpression(graph, valueNode, row);
        for ( int i = 1 ; i < args.size(); i++ ) {
            Node arg = args.get(i);
            NodeValue nv = NodeExpressions.evalNodeExpression(graph, arg, row);
            if ( nv == null )
                throw new InternalErrorException("Node expression return null");
            if ( NodeValue.sameValueAs(value, nv) )
                return true;
        }
        return false;
    }

    static NodeValue sparql_bound(Graph graph, Node callNode, FunctionEnv functionEnv, Binding row, Node arg1) {
        Expr expr1 =  SparqlNodeExpressions.fromExpr(graph, arg1);
        if ( ! expr1.isVariable() )
            throw new NodeExprEvalException("Argument to sh:bound is not a variable");
        // Just do it!
        boolean b =  row.contains(expr1.getExprVar().asVar());
        return NodeValue.booleanReturn(b);
        // Purist - except E_Bound accepts constants (Jena5: currently)
//        Expr x = new E_Bound(expr1);
//        return x.eval(row, functionEnv);
    }

    // EXISTS, NOT EXISTS
    //entry(mapDispatch, mapBuild, "sparql:filter-exists", E_Exists.class, "EXISTS", E_Exists::new, SPARQLFuncOp::filter-exists );
    //entry(mapDispatch, mapBuild, "sparql:filter-not-exists", E_NotExists.class, "NOT EXISTS", E_NotExists::new, SPARQLFuncOp::filter-not-exists );

    // URI function call.
    //entry(mapDispatch, mapBuild, "sparql:function", E_Function.class, "", E_Function::new, SPARQLFuncOp::function );
}
