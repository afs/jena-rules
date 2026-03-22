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
import org.seaborne.jena.shacl_rules.RulesException;

public class TupleStoreZero implements TupleStore {

    public static TupleStoreZero create() {
        return new TupleStoreZero();
    }

    @Override
    public boolean contains(Tuple tuple) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void add(Tuple tuple) {
        throw new RulesException("Can't add tuple to "+this.getClass().getSimpleName());
    }

    @Override
    public void delete(Tuple tuple) {
        throw new RulesException("Can't delete tuple to "+this.getClass().getSimpleName());
    }

    @Override
    public Iterator<Tuple> find(Tuple pattern) {
        return Iter.nullIterator();
    }

    @Override
    public Iterator<Tuple> all() {
        return Iter.nullIterator();
    }
}
