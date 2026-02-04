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

package org.seaborne.jena.shacl_rules.tuples0.rel;

import java.util.List;
import java.util.StringJoiner;

import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.atlas.lib.tuple.TupleFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.sse.SSE;

/**
 * Strictly : in datalog, this is a "Literal", but that is confusing for RDF,
 * or "Atom", yet it is composite.
 */
public class Rel  {
    private final String name;
    private final Tuple<Node> tuple;

    public static Rel fromTriple(Triple triple) {
        return new Rel("", TupleFactory.create3(triple.getSubject(), triple.getPredicate(), triple.getObject()));
    }

    public static Triple toTripleAny(Rel rel) {
        if ( rel.len() != 3 )
            throw new IllegalArgumentException("Rel.toTriple: arg not of length 3: "+rel);
        if ( !rel.name.isEmpty() )
            throw new IllegalArgumentException("Rel.toTriple: name is not \"\"");
        return Triple.create(any(rel.get(0)), any(rel.get(1)), any(rel.get(2)));

    }
    private static Node any(Node node) {
        if ( node == null ) return Node.ANY;
        if ( Var.isVar(node) ) return Node.ANY;
        return node;
    }

    public static Triple toTriple(Rel rel) {
        if ( rel.len() != 3 )
            throw new IllegalArgumentException("Rel.toTriple: arg not of length 3: "+rel);
        if ( !rel.name.isEmpty() )
            throw new IllegalArgumentException("Rel.toTriple: name is not \"\"");
        return Triple.create(rel.get(0), rel.get(1), rel.get(2));
    }

    public Rel(String name, Tuple<Node> tuple) {
        this.name = name;
        this.tuple = tuple;
    }

    public Rel(String name, Node...nodes) {
        this(name, TupleFactory.tuple(nodes));
    }

    public Rel(String name, List<Node> nodes) {
        this(name, TupleFactory.create(nodes));
    }

    public String getName() {
        return name;
    }

    public Node get(int idx) {
        return tuple.get(idx);
    }
    public Tuple<Node> getTuple() {
        return tuple;
    }

    public boolean isConcrete() {
        for ( int i = 0 ; i < tuple.len() ; i++) {
            if ( ! tuple.get(i).isConcrete() )
                return false ;
        }
        return true;
    }

    public int len() {
        return tuple.len();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((tuple == null) ? 0 : tuple.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        Rel other = (Rel)obj;
        if ( name == null ) {
            if ( other.name != null )
                return false;
        } else if ( !name.equals(other.name) )
            return false;
        if ( tuple == null ) {
            if ( other.tuple != null )
                return false;
        } else if ( !tuple.equals(other.tuple) )
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", name+"(", ")");
        tuple.forEach(n->sj.add(SSE.str(n)));
        return sj.toString();
    }
}
