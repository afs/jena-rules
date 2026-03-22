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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.tuples.Tuple;

public class RuleHead { //implements Iterable<RuleHeadElement>{

    private final List<RuleHeadElement> head;
    private final List<Triple> triples = new ArrayList<>();
    private final List<Tuple> tuples = new ArrayList<>();

    public RuleHead(List<RuleHeadElement> elements) {
        this.head = Collections.unmodifiableList(elements);
        boolean seenTriple = false;
        boolean seenTuple = false;

        for ( int i = 0 ; i < head.size() ; i++ ) {
            RuleHeadElement elt = head.get(i);
            switch (elt) {
                case RuleHeadElement.EltTripleTemplate(Triple tripleTemplate) -> { triples.add(tripleTemplate); }
                case RuleHeadElement.EltTupleTemplate(Tuple tupleTemplate) -> { tuples.add(tupleTemplate); }
                default ->{}
            }
        }
    }

    public boolean hasTriples() { return ! triples.isEmpty(); }
    public boolean hasTuples() { return ! tuples.isEmpty(); }

    public List<RuleHeadElement> getHeadElements() {
        return head;
    }

    // XXX This should go away!
    public List<Triple> getHeadTriples() { return triples; }

    public List<Tuple> getHeadTuples() { return tuples; }

    public void forEach(Consumer<RuleHeadElement> action) {
        head.forEach(action);
    }

    /**
     * Apply an action to each index and rule, in the order they appear in the body.
     */
    public void forEach(BiConsumer<Integer, RuleHeadElement> action) {
        int N = head.size();
        for ( int i = 0 ; i < N ; i++ ) {
            action.accept(i, head.get(i));
        }
    }

    /**
     * Equivalent - same effect, not necessarily {@code .equals}.
     * Same triples, any order.
     */
    public boolean equivalent(RuleHead other) {
        return ListUtils.equalsUnordered(this.head, other.head);
    }

    @Override
    public int hashCode() {
        return Objects.hash(head);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( !(obj instanceof RuleHead) )
            return false;
        RuleHead other = (RuleHead)obj;
        return Objects.equals(head, other.head);
    }

    @Override
    public String toString() {
        return head.toString();
    }
}
