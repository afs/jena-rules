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

package org.seaborne.jena.shacl_rules.tuples;

import java.util.Map;
import java.util.StringJoiner;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.sse.SSE;

public class Tuples {

    public static Tuple substitute(Tuple tuple, Binding binding) {
        if ( isNotNeeded(binding) )
            return tuple;
        if ( tuple.isConcrete() )
            return tuple;
        int N = tuple.size();
        Node[] terms = new Node[N];
        boolean changed = false;
        for ( int i = 0 ; i < N ; i++ ) {
            Node n1 = tuple.get(i);
            // This deals with triple terms.
            Node n2 = Substitute.substitute(n1, binding);
            if ( ! n1.sameTermAs(n2) )
                changed = true;
            terms[i] = n2;
        }
        if ( ! changed )
            return tuple;
        return Tuple.create(terms);
    }

    public static Tuple substituteTemplate(Tuple tuple, Binding binding, Map<Node, Node> bNodeMap) {
        if ( isNotNeeded(binding) )
            return tuple;
        if ( tuple.isConcrete() )
            return tuple;
        int N = tuple.size();
        Node[] terms = new Node[N];
        boolean changed = false;
        for ( int i = 0 ; i < N ; i++ ) {
            Node n1 = tuple.get(i);
            if ( n1.isBlank() ) {
                changed = true;
                n1 = newBlank(n1, bNodeMap);
            }
            // This deals with triple terms.
            Node n2 = Substitute.substitute(n1, binding);
            if ( ! n1.sameTermAs(n2) )
                changed = true;
            terms[i] = n2;
        }
        if ( ! changed )
            return tuple;
        return Tuple.create(terms);
    }

    //DRY: TemplateLib
    /** generate a blank node consistently */
    private static Node newBlank(Node n, Map<Node, Node> bNodeMap) {
        if ( !bNodeMap.containsKey(n) )
            bNodeMap.put(n, NodeFactory.createBlankNode());
        return bNodeMap.get(n);
    }


    private static boolean isNotNeeded(Binding b) {
        return b == null || b.isEmpty();
    }

    public static String displayStr(Tuple tuple) {
        StringJoiner sj = new StringJoiner(" ", "$(", ")");
        for ( Node n : tuple )
            sj.add(NodeFmtLib.displayStr(n));
        return sj.toString();
    }

    // Development helper
    public static Tuple createTuple(String...strings) {
        Node[] terms = new Node[strings.length];
        for ( int i = 0 ; i < strings.length ; i++ ) {
            terms[i] = SSE.parseNode(strings[i]);
        }
        return Tuple.create(terms);
    }
}
