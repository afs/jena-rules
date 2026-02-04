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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.jena.atlas.iterator.Iter;
import org.seaborne.jena.shacl_rules.tuples0.rel.Rel;
import org.seaborne.jena.shacl_rules.tuples0.rel.RelStore;
import org.seaborne.jena.shacl_rules.tuples0.rel.RelStoreBuilder;

/**
 * Simple {@link RelStore}, uses a MultiValuedMap of atom name to index.
 * Useful as an independent implementation for testing.
 */
public class RelStoreSimple extends RelStoreBase {

    public static RelStoreBuilder create() {
        return new Builder();
    }

    static class Builder implements RelStoreBuilder {
        //private MultiValuedMap<String,Rel> store = ArrayListMultiValuedMap.create();
        private MultiValuedMap<String,Rel> store = MultiMapUtils.newListValuedHashMap();

        @Override
        public Builder add(Rel rel) {
            //checkUpdatable(()->"add("+rel+")") ;
            store.put(rel.getName(), rel);
            return this;
        }

        @Override
        public Builder delete(Rel rel) {
            //checkUpdatable(()->"delete("+rel+")") ;
            store.removeMapping(rel.getName(), rel);
            return this;
        }

        @Override
        public RelStore build() {
            // Isolate.
            MultiValuedMap<String,Rel> store2 = new ArrayListValuedHashMap<>(store);
            RelStore result = new RelStoreSimple(store2);
            return result;
        }
    }

    private final MultiValuedMap<String,Rel> store;

    private RelStoreSimple(MultiValuedMap<String,Rel> store) {
        this.store = new ArrayListValuedHashMap<>(store);
    }

    @Override
    public Iterator<Rel> find(Rel rel) {
        // Materializing for simplicity
        if ( !store.containsKey(rel.getName()))
            return Iter.nullIterator();
        List<Rel> result = new ArrayList<>();
        Collection<Rel> x = store.get(rel.getName());
        for(Rel r : x) {
            if ( match(r,rel) )
                result.add(r);
        }
        return result.iterator();
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public long size() {
        return store.size();
    }

    @Override
    public boolean contains(Rel rel) {
        if ( !store.containsKey(rel.getName()))
            return false;
        return store.get(rel.getName()).contains(rel);
    }

    @Override
    public boolean matches(Rel rel) {
        if ( !store.containsKey(rel.getName()))
            return false;
        Collection<Rel> x = store.get(rel.getName());
        // Does not work! CCME!
        //return x.stream().anyMatch(r-> match(r,rel));
        for(Rel r : x) {
            if ( match(r,rel) )
                return true;
        }
        return false;
    }

    @Override
    public Stream<Rel> stream() {
        return store.values().stream();
    }
}
