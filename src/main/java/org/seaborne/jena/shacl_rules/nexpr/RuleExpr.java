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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction;

/**
 * This is not a public API.
 * 
 * Entry points for other parts of the jena-rules system to get to node expression functionality.
 */

/*public*/
/*package*/ class RuleExpr {

    /**
     *  Function call: URI of a registers node expression, and the arguments, returning a callable SPARQL {@link Expr}.
     */
    public static Expr build(String functionURI, Expr[] args) {
        NodeExprTables.BuildSyntax build = NodeExprTables.getBuild(functionURI);
        if ( build == null )
            throw new RuleExprEvalException("Build: "+functionURI);
        Expr expr = build.build(functionURI, args);
        return expr;
    }

    /**
     * For a given SPARQL function, or functional form,
     * return the URI for a SPARQL function call.
     * This intercepts general SPARQL {@link Expr} call-by-URI
     * to call a NodeExprTables registration.
     * Return the native URI is not registered.
     */
    public static Node exprFunctionURI(ExprFunction exf, int arity) {
        String uri = NodeExprTables.getUriForExpr(exf);
        if ( uri != null )
            return NodeFactory.createURI(uri);
        // A URI for the function.
        return NodeFactory.createURI(exf.getFunctionIRI());
    }
}
