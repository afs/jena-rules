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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.rdf_syntax.RVar;
import org.seaborne.jena.shacl_rules.sys.P;

/**
 * Helper functions for working with node expressions.
 * @see NodeExpressions for the main API including evaluation.
 */
public class NX {

    public static final String SHNEX = P.SHNEX;

    public static final Node var = uri("var");

    private static Node uri(String localName) { return uri(SHNEX, localName); }
    private static Node uri(String namespace, String localName) { return NodeFactory.createURI(namespace+localName); }

    /**
     * Return the name of the variable at the given node.
     * Return null for "not a variable".
     */
    public static String getVarName(Graph graph, Node node) {
        // Am I a variable?
        Node vx = G.getZeroOrOneSP(graph, node, NX.var);
        if ( vx == null )
            return null;
        // RDFDataException if it is not a string.
        String vName = G.asString(vx);
        return vName;
    }

    /** @see RVar */
    public static Var getVar(Graph graph, Node node) {
        // Accepts both forms.
        return RVar.getVar(graph, node);
    }

//    /**
//     * Return the name of the variable at the given node.
//     * Return null for "not a variable".
//     * Only accepts {@code shnex:var}
//     */
//    public static Var getVar(Graph graph, Node node) {
//        String vName = getVarName(graph, node);
//        if ( vName == null )
//            return null;
//        Var var = Var.alloc(vName);
//        return var;
//    }

    /**
     * Update the graph to put in a variable as
     * {@code [ sh:var "varName" ]}.
     * Return the block node created for the variable.
     */
    public static Node addVar(Graph graph, String varName) {
        Node x = NodeFactory.createBlankNode();
        Node str = NodeFactory.createLiteralString(varName);
        graph.add(x, NX.var, str);
        return x;
    }

    /**
     * Get the triple for a call.
     */
    static Triple getFunctionTriple(Graph graph, Node exprNode) {
        // Named argument function forms ...
        List<Triple> triples = G.find(graph, exprNode, null, null).toList();

        if ( triples.isEmpty() )
            return null;
        if ( triples.size() == 1 )
            // List argument function form.
            return triples.getFirst();
        // See whether it is a named argument form:
        for ( Triple t : triples ) {
            Node p = t.getPredicate();
            if ( NodeExpressions.namedNodeExpressions.contains(p)) {
                return t;
            }
        }
        // - Not named a recognized named argument expression,
        // - Not the syntax of a list argument function/functional form.
        throw new NodeExprEvalException("Multiple triples for function: "+triples);
    }

    /**
     * Get an RDF node expression for a list argument node expression function.
     */
    public static NodeExpressionFunction getRDFExpression(Graph graph, Node root) {
        Triple t = getFunctionTriple(graph, root);
        Node pFunction = t.getPredicate();
        if ( ! pFunction.isURI( ) )
            throw new ShaclException("Not a URI for a node expression function");
        Node o = t.getObject();
        List<Node> list = G.rdfList(graph, o);
        return new NodeExpressionFunction(pFunction.getURI(), list);
    }

}
