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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.apache.jena.atlas.iterator.Iter;
import org.seaborne.jena.shacl_rules.tuples.Tuple;
import org.seaborne.jena.shacl_rules.tuples.Tuples;
import org.seaborne.jena.shacl_rules.tuples.TupleStore;

public class TestTupleStore {

    private static void print(List<Tuple> r) {
        System.out.println("[");
        r.forEach(t->System.out.println("  "+t));
        System.out.println("]");
    }

    static private TupleStore store() { return TupleStore.create(); }

    @Test public void tupleStore_01() {
        TupleStore store = store();
    }

    @Test public void tupleStore_02() {
        Tuple tuple1 = Tuples.createTuple(":x");
        TupleStore store = store();
        store.add(tuple1);
        assertEquals(1, store.size());
        store.delete(tuple1);
        assertEquals(0, store.size());
    }

    @Test public void tupleStore_03() {
        Tuple tuple1 = Tuples.createTuple(":x");
        Tuple tuple2 = Tuples.createTuple(":y");
        Tuple pattern1 = Tuples.createTuple(":x");
        Tuple pattern2 = Tuples.createTuple("_");

        TupleStore store = store();
        store.add(tuple1);
        store.add(tuple2);

        {
            Iterator<Tuple> iter1 = store.find(pattern1);
            assertEquals(1, Iter.count(iter1), "Pattern1");
        }
        {
            Iterator<Tuple> iter2 = store.find(pattern2);
            assertEquals(2, Iter.count(iter2), "Pattern2");
        }
        store.delete(tuple2);

        {
            Iterator<Tuple> iter3 = store.find(pattern1);
            assertEquals(1, Iter.count(iter3));
        }

        store.delete(tuple1);
        {
            Iterator<Tuple> iter4 = store.find(pattern1);
            assertEquals(0, Iter.count(iter4));
        }

        assertEquals(0, store.size());
    }
}
