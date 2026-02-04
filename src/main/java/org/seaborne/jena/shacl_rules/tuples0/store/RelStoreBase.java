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

package org.seaborne.jena.shacl_rules.tuples0.store;

import java.util.StringJoiner;
import java.util.stream.Stream;

import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.graph.Node;
import org.seaborne.jena.shacl_rules.tuples0.rel.Rel;
import org.seaborne.jena.shacl_rules.tuples0.rel.RelStore;

/**
 * Framework for writing a {@link RelStore}. Provide as many of the operations by building
 * on top of some abstract ones.
 *
 * @apiNote There is no assumption that the provided implementations are the most
 *          appropriate or most efficient.
 */
public abstract class RelStoreBase implements RelStore {

    /** Get all the Rels matching a name*/
    @Override
    public Stream<Rel> get(String relName) {
        return stream().filter(r->r.getName().equals(relName));
    }

    /** Does this RelStore have any rels of a given name? */
    @Override
    public boolean containRel(String relName) {
        return get(relName).anyMatch(r->r.getName().equals(relName));
    }

    @Override
    public boolean isEmpty() {
        return !stream().findAny().isPresent();
    }

    @Override
    public long size() {
        return stream().count();
    }

    @Override
    public boolean contains(Rel rel) {
        return rel.isConcrete() && matches(rel);
    }

    @Override
    public boolean matches(Rel rel) {
        return find(rel).hasNext();
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(" . ");
        stream().forEach(r -> sj.add(r.toString()));
        return sj.toString();
    }

    /** Test whether {@code Rel} {@code item} satifises {@code Rel} {@code pattern} */
    protected static boolean match(Rel item, Rel pattern) {
        Tuple<Node> data = item.getTuple();
        Tuple<Node> pat = pattern.getTuple();
        if ( data.len() != pat.len() )
            return false;
        for ( int i = 0 ; i < data.len() ; i++ ) {
            Node n1 = data.get(i);
            Node n2 = pat.get(i);
            if ( !match(n1, n2) )
                return false;
        }
        return true;
    }

    /**
     * Test whether Node {@code n1} matches Node {@code n2} where "matches" means one or
     * both of {@code n1} and {@code n2} are wildcards or {@code n1.equals(n2)}.
     */
    protected static boolean match(Node n1, Node n2) {
        if ( wildcard(n1) || wildcard(n2) )
            return true;
        return n1.equals(n2);
    }

    /** Is this a wildcard? (Variable or ANY) */
    protected static boolean wildcard(Node n) {
        return Node.ANY.equals(n) || n.isVariable();
    }
}
