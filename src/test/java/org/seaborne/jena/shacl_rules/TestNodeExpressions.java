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

package org.seaborne.jena.shacl_rules;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.expr.NX;
import org.seaborne.jena.shacl_rules.expr.NodeExpressionFunction;
import org.seaborne.jena.shacl_rules.expr.NodeExpressions;
import org.seaborne.jena.shacl_rules.sys.P;
import org.seaborne.jena.shacl_rules.sys.V;

public class TestNodeExpressions {

    private static String PREFIXES = P.PREFIXES+"PREFIX : <http://example/>\n";

    @Test public void nx_expr_uri() {
        String nxGraph = PREFIXES+"""
                :nx shr:expr [ sparql:now () ] .
                """;
        Node nx = NodeFactory.createURI("http://example/nx");
        Graph graph = RDFParser.fromString(nxGraph, Lang.TTL).toGraph();

        NodeValue nv = NodeExpressions.evalNodeExpression(graph, nx);
        assertNotNull(nv);
    }

    @Test public void nx_expr_bnode() {
        String nxGraph = PREFIXES+"""
                [] shr:expr [ sparql:now () ] .
                """;
        Graph graph = RDFParser.fromString(nxGraph, Lang.TTL).toGraph();
        Node nx = G.getOneSP(graph, null,  V.expr);

        NodeExpressionFunction nef = NX.getRDFExpression(graph, nx);
        assertNotNull(nef);
        assertNotNull(nef.arguments());
        assertNotNull(nef.uri());

        NodeValue nv = NodeExpressions.evalNodeExpression(graph, nx);
        assertNotNull(nv);
    }

    // Define a test function.
}
