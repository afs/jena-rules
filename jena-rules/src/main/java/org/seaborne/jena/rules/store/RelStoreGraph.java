/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.jena.rules.store;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.seaborne.jena.rules.Rel;

/** A read-only {RelStore} of RDF Triples */
public class RelStoreGraph extends RelStoreBase {

    private final Graph graph;

    public RelStoreGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Iterator<Rel> find(Rel rel) {
        Triple triple = Rel.toTripleAny(rel);
        ExtendedIterator<Triple> iter = graph.find(triple);
        return iter.mapWith(Rel::fromTriple);
    }

    @Override
    public Stream<Rel> stream() {
        ExtendedIterator<Triple> iter = graph.find();
        ExtendedIterator<Rel> rels = iter.mapWith(Rel::fromTriple);
        return Iter.asStream(rels);
    }
}
