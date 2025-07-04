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

package org.seaborne.jena.shacl_rules.jena;

import java.util.Iterator;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.TransactionHandler;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.JenaException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.graph.GraphWrapper;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;

/**
 * A read-only graph that adds access to a second graph
 * that is accessed in addition to the main graph.
 * <p>
 * The expected use is that the left graph is additional triples being added to a base graph.
 */
public class Graph2 extends GraphWrapper {

    private final Graph additionalGraph;
    private final PrefixMapping prefixMapping;
    private final boolean disjoint;

    public static Graph create(Graph extraGraph, Graph baseGraph) {
        if ( extraGraph instanceof Graph2)
            Log.warn(Graph2.class, "Combining a Graph2 with a Graph2.");
        if ( baseGraph instanceof Graph2)
            Log.warn(Graph2.class, "Creating a Graph2 over a Graph2 base graph.");
        return new Graph2(extraGraph, baseGraph);
    }

    protected Graph2(Graph extraGraph, Graph baseGraph) {
        super(baseGraph);
        final boolean disjoint = false; // For now ...
        this.additionalGraph = extraGraph;
        this.prefixMapping = setupPrefixMapping(baseGraph.getPrefixMapping());
        this.disjoint = disjoint;
    }

    private static PrefixMapping setupPrefixMapping(PrefixMapping baseGraphPrefixes) {
        PrefixMapping basePrefixes = baseGraphPrefixes;
        // XXX
        // Copy to isolate.
        basePrefixes = new PrefixMappingImpl().setNsPrefixes(basePrefixes);
        // FIXME
        PrefixMapping prefixMapping = new AppendPrefixMapping(basePrefixes);
        return prefixMapping;
    }

    public Graph base() { return get(); }

    protected Graph additionalGraph() { return additionalGraph; }

    // ---- Graph2 is read-only: Update operations not provided

    @Override
    public void add(Triple t) {
        throw new AddDeniedException("Graph2::add");
    }

    @Override
    public void delete(Triple t) {
        throw new DeleteDeniedException("Graph2::delete");
    }

    @Override
    public void clear() {
        throw new JenaException("Graph2::clear");
    }

    @Override
    public void remove( Node s, Node p, Node o ) {
        throw new JenaException("Graph2::remove");
    }

    // XXX Transaction handler

    // ---- Read access.

    @Override
    public TransactionHandler getTransactionHandler() {
        // XXX ???
        return super.getTransactionHandler();
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return prefixMapping;
    }

    @Override
    public boolean contains(Node s, Node p, Node o) {
        return contains(Triple.create(s, p, o));
    }

    @Override
    public boolean contains(Triple triple) {
        if ( additionalGraph.contains(triple) )
            return true;
        Graph base = get();
        return base.contains(triple);
    }

    @Override
    public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
        Iterator<Triple> extra = findInAdded(s, p, o);
        Iter<Triple> iter =
            Iter.iter(get().find(s, p, o))
                .append(extra);
        if ( ! disjoint )
            iter = iter.distinct();
        return WrappedIterator.create(iter);
    }

    private Iterator<Triple> findInAdded(Node s, Node p, Node o) {
        return additionalGraph.find(s,p,o);
    }

    @Override
    public ExtendedIterator<Triple> find(Triple m) {
        return find(m.getMatchSubject(), m.getMatchPredicate(), m.getMatchObject());
    }

    @Override
    public boolean isEmpty() {
        Graph base = get();
        return additionalGraph.isEmpty() && base.isEmpty();
    }

    @Override
    public int size() {
        if ( disjoint )
            return super.size() + additionalGraph.size();
        return (int)(Iter.count(find(Triple.ANY)));
    }
}
