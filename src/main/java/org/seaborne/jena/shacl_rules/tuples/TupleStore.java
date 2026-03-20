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

public interface TupleStore {

    public static TupleStore create() {
        return new TupleStoreSimple();
    }

    /** Test for a concrete occurence of tuple (no patterns) */
    public boolean contains(Tuple tuple);

    public long size();

    public void add (Tuple tuple);

    /** Delete one occurrence of Tuple. */
    public void delete (Tuple tuple);

    /** Delete all occurrences of Tuple. */
    public void deleteAll (Tuple tuple);

    public Iterator<Tuple> find(Tuple pattern);


//    public Iterator<Tuple> find(Node node1);
//    public Iterator<Tuple> find(Node node1, Node node2);
//    public Iterator<Tuple> find(Node node1, Node node2, Node node3);
//    public Iterator<Tuple> find(Node... nodes);


}
