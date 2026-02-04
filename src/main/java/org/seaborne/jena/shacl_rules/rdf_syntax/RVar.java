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

package org.seaborne.jena.shacl_rules.rdf_syntax;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.expr.NX;
import org.seaborne.jena.shacl_rules.sys.V;

/**
 * RDF-encoing of variables - from a rule perspective.
 * <p>
 * {@code shr:varName} is synonym for {@code shnex:var}
 * <p>
 * Accept both when reading; write {@code shr:varName} in triple patterns and
 * templates, where it is not node expression evaluation
 * (e.g. there is no "scope"),
 * but write {@code shnex:var} with node expressions (e.g conditions)
 */
public class RVar {
    // Accept both, write one.
    // Write shr:varName in triple patterns and templates
    // Write shnex:var in node expressions

    private static Node predicateSHR = V.varName;
    private static Node predicateNX = NX.var;

    // Preferred predicate writing in triple patterns and templates.
    /** {@link RVar#addVar} here */
    private static Node varPatternPredicate = predicateSHR;

    // Preferred predicate writing in node expressions.
    /** {@link NX#addVar}. */
    private static Node varExprPredicate = predicateNX;

    /**
     * Return the name of the variable at the given node.
     * Return null for "not a variable".
     * Accepts either {@code shr:varName} or {@code shnex:var}.
     */
    public static String getVarName(Graph graph, Node node) {
        // Am I a variable?
        Node vx = G.getZeroOrOneSP(graph, node, predicateSHR);
        if ( vx == null )
            vx = G.getZeroOrOneSP(graph, node, predicateNX);
        if ( vx == null )
            return null;
        // RDFDataException if it is not a string.
        String vName = G.asString(vx);
        return vName;
    }

    /**
     * Return the name of the variable at the given node.
     * Return null for "not a variable".
     * Accepts either {@code shr:varName} or {@code shnex:var}.
     */
    public static Var getVar(Graph graph, Node node) {
        String vName = getVarName(graph, node);
        if ( vName == null )
            return null;
        Var var = Var.alloc(vName);
        return var;
    }

    /**
     * Update the graph to put in a variable as
     * {@code [ srl:varName "varName" ]}.
     * Return the block node created for the variable.
     */
    public static Node addVar(Graph graph, String varName) {
        Node x = NodeFactory.createBlankNode();
        Node str = NodeFactory.createLiteralString(varName);
        graph.add(x, varPatternPredicate, str);
        return x;
    }
}
