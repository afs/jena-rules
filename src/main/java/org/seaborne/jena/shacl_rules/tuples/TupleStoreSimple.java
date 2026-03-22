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

import java.util.*;

import org.apache.jena.graph.Node;

// VERY simple!
public class TupleStoreSimple implements TupleStore {

    // Split by arity.
    private final Set<Tuple> tupleSet = new LinkedHashSet<>();

    public TupleStoreSimple() {}

    public TupleStoreSimple(Collection<Tuple> tuples) {
        tupleSet.addAll(tuples);
    }

    @Override
    public boolean contains(Tuple tuple) {
        checkConcrete(tuple);
        return false;
    }

    @Override
    public int size() {
        return tupleSet.size();
    }

    @Override
    public void add(Tuple tuple) {
        checkConcrete(tuple);
        tupleSet.add(tuple);
    }

    @Override
    public void addAll(Collection<Tuple> tuples) {
        // Via checking.
        tuples.forEach(this::add);
    }

    @Override
    public void addAll(TupleStore other) {
        other.all().forEachRemaining(this::add);
    }

    @Override
    public void delete(Tuple tuple) {
        checkConcrete(tuple);
        tupleSet.remove(tuple);
    }

    @Override
    public Iterator<Tuple> find(Tuple pattern) {
        Objects.requireNonNull(pattern);
        List<Tuple> results = new ArrayList<>();
        for ( Tuple tuple : tupleSet ) {
            if ( match(pattern, tuple) )
                results.add(tuple);
        }
        return results.iterator();
    }

    @Override
    public Iterator<Tuple> all() {
        return tupleSet.iterator();
    }

    private boolean match(Tuple pattern, Tuple tuple) {
        if ( pattern.size() != tuple.size() )
            return false;
        for ( int i = 0 ; i < pattern.size() ; i++ ) {
            if ( !match(pattern.get(i), tuple.get(i)) )
                return false;
        }
        return true;
    }

    private boolean match(Node node1, Node node2) {
        if ( ! node1.isConcrete() )
            return true;
        if ( ! node2.isConcrete() )
            return true;
        return node1.sameTermAs(node2);
    }

    private static void checkConcrete(Tuple tuple) {
        if ( tuple == null )
            throw new NullPointerException("Tuple");
        noNulls(tuple);
        if ( ! tuple.isConcrete() )
            throw new IllegalArgumentException("Tuple not concrete");
    }

    private static void noNulls(Tuple tuple) {
        for ( int i = 0 ; i < tuple.size() ; i++ ) {
            Node x = tuple.get(i);
            if ( x == null )
                throw new NullPointerException("Tuple term");
        }
    }
}
