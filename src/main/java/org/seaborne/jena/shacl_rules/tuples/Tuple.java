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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.sse.SSE;

public class Tuple {

    public static Tuple create(Node...terms) {
        return new Tuple(terms);
    }

    public static Tuple create(List<Node> terms) {
        return new Tuple(terms);
    }

    public static Tuple create(String...strings) {
        Node[] terms = new Node[strings.length];
        for ( int i = 0 ; i < strings.length ; i++ ) {
            terms[i] = SSE.parseNode(strings[i]);
        }
        return new Tuple(terms);
    }

    private final List<Node> items;

    private Tuple(Node...terms) {
        Objects.requireNonNull(terms);
        this.items = Arrays.asList(terms);
    }

    private Tuple(List<Node> terms) {
        Objects.requireNonNull(terms);
        this.items = List.copyOf(terms);
    }

    public int size() {
        return items.size();
    }

    public void forEach(Consumer<Node> action) {
        items.forEach(action);
    }

    public Node get(int i) {
        return items.get(i);
    }

    public boolean isConcrete() {
        for ( Node n : items ) {
            if ( Var.isVar(n) )
                return false;
            if ( Node.ANY.equals(n) )
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for ( Node n : items ) {
            sj.add(NodeFmtLib.displayStr(n));
        }
        return sj.toString() ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( !(obj instanceof Tuple) )
            return false;
        Tuple other = (Tuple)obj;
        return Objects.equals(items, other.items);
    }


}