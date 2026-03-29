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

import java.util.Iterator;

import org.apache.jena.atlas.iterator.Iter;

/**
 *
 * A {@link TupleStore} that records added tuples.
 * {@link #delete} is not supported.
 */
public class AppendTupleStore implements TupleStore {
    private final TupleStore additions = TupleStore.create();
    private final TupleStore baseStore;
    public static AppendTupleStore create(TupleStore baseTupleStore) {
        return new AppendTupleStore(baseTupleStore);
    }

    public AppendTupleStore(TupleStore baseTupleStore) {
        this.baseStore = baseTupleStore;
    }

    public TupleStore getAdded() { return additions; }

    @Override
    public boolean contains(Tuple tuple) {
        return additions.contains(tuple) || baseStore.contains(tuple);
    }

    @Override
    public int size() {
        return additions.size()+baseStore.size();
    }

    @Override
    public void add(Tuple tuple) {
        if ( ! baseStore.contains(tuple) )
            additions.add(tuple);
    }

    @Override
    public void delete(Tuple tuple) {
        new UnsupportedOperationException();
    }

    @Override
    public Iterator<Tuple> find(Tuple pattern) {
        // A TupleStore is a set of tuples.
        Iterator<Tuple> iter1 = additions.find(pattern);
        Iterator<Tuple> iter2 = baseStore.find(pattern);
        return concatDistinct(iter1, iter2);
    }

    @Override
    public Iterator<Tuple> all() {
        Iterator<Tuple> iter1 = additions.all();
        Iterator<Tuple> iter2 = baseStore.all();
        return concatDistinct(iter1, iter2);
    }

    private static  <X> Iterator<X> concatDistinct(Iterator<X> iter1, Iterator<X> iter2) {
        if ( iter1 == null )
            return iter2;
        if ( iter2 == null )
            return iter1;
        return Iter.iter(iter1).append(iter2).distinct();
    }
}
