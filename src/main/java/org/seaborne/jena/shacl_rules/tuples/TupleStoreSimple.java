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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.jena.graph.Node;

// VERY simple!
public class TupleStoreSimple implements TupleStore {

    // Split by arity.
    private List<Tuple> tuples = new ArrayList<>();

    @Override
    public boolean contains(Tuple tuple) {
        checkConcrete(tuple);
        return false;
    }

    @Override
    public long size() {
        return tuples.size();
    }

    @Override
    public void add(Tuple tuple) {
        checkConcrete(tuple);
        tuples.add(tuple);
    }

    @Override
    public void delete(Tuple tuple) {
        checkConcrete(tuple);
        tuples.remove(tuple);
    }

    @Override
    public Iterator<Tuple> find(Tuple pattern) {
        Objects.requireNonNull(pattern);
        List<Tuple> results = new ArrayList<>();
        for ( Tuple tuple : tuples ) {
            if ( match(pattern, tuple) )
                results.add(tuple);
        }
        return results.iterator();
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

    @Override
    public void deleteAll(Tuple tuple) {
        while(tuples.remove(tuple)) {}
    }

    private static void checkConcrete(Tuple tuple) {
        if ( tuple == null )
            throw new NullPointerException("Tuple");
        if ( ! tuple.isConcrete() )
            throw new IllegalArgumentException("Tuple not concrete");
    }
}
