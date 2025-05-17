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

import static org.apache.jena.system.G.containsBySameTerm;
import static org.apache.jena.system.G.execTxn;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMemFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.system.buffering.BufferingCtl;

/*
 * An {@link AppendGraph} is a special case of a {@link BufferingGraph}.
 * It only supports adds adding triples, not delete.
 * <p>
 * It can be used, then the accumulated changes flushed to the base graph or
 * uses for temporary workspace and the change thrown away.
 */
//public class AppendGraph extends GraphWrapper implements BufferingCtl {

public class AppendGraph extends Graph2 implements BufferingCtl {

    // "Sometime" this could be made the super class of BufferingGraph.

    // Controls whether to check the underlying graph to check
    // whether to record a change or not.
    //
    // If true, the added graph does not contain triples in the base graph.
    // The AppendGraph acts as a diff of the changes and the base graph
    //
    // If false, the added graph may contain duplicates of the base graph.
    // The AppendGraph acts as a record of all additions.
    // It takes more memory but means the underlying graph is not touched for add().

    private final boolean checkOnUpdate;

    // Controls whether flushing to the underlying graph is allowed.
    // If not allowed, this class guarantee it will not update the base graph.
    // Combined with checkOnUpdate=false, the base graph is not touched at all on
    // update, only for read use.

    private final boolean allowFlush;
    private final AppendPrefixMapping appendPrefixMapping;

    public static AppendGraph create(Graph graph) {
        if ( graph instanceof AppendGraph )
            Log.warn(Graph2.class, "Creating a AppendGraph over an AppendGraph");
        return new AppendGraph(graph, true, false);
    }

    // Better : getPrefixMapping

    private AppendGraph(Graph graph, boolean checkOnUpdate, boolean allowFlush) {
        super(addedTriplesGraph(), graph);
        this.checkOnUpdate = checkOnUpdate;
        this.allowFlush = allowFlush;
        PrefixMapping basePrefixes = graph.getPrefixMapping();
        if ( allowFlush )
            // Copy to isolate.
            basePrefixes = new PrefixMappingImpl().setNsPrefixes(basePrefixes);
        appendPrefixMapping = new AppendPrefixMapping(basePrefixes);
    }

    // Rename the graph used for added triples.
    private Graph addedGraph() {
        return super.additionalGraph();
    }

    private static Graph addedTriplesGraph() {
        return GraphMemFactory.createDefaultGraph();
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return appendPrefixMapping;
    }

    /** Flush the changes to the base graph, using a Graph transaction if possible. */
    @Override
    public void flush() {
        if ( allowFlush )
            throw new UnsupportedOperationException(this.getClass().getSimpleName()+".flush");
        Graph base = get();
        execTxn(base, ()-> flushDirect(base));
    }

    /** Flush the changes directly to the base graph. */
    public void flushDirect() {
        if ( allowFlush )
            throw new UnsupportedOperationException(this.getClass().getSimpleName()+".flushDirect");
        // So that get() is called exactly once per call.
        Graph base = get();
        flushDirect(base);
    }

    private void flushDirect(Graph base) {
        if ( allowFlush )
            throw new UnsupportedOperationException(this.getClass().getSimpleName()+".flush");
        addedGraph().find().forEachRemaining(base::add);
        addedGraph().clear();
        appendPrefixMapping.flush();
    }

//    private void updateOperation() {}
//
//    private void readOperation() {}

    @Override
    public void add(Triple t) {
        execAdd(t);
    }

    @Override
    public void delete(Triple t) {
        execDelete(t);
    }

    private void execAdd(Triple triple) {
        //updateOperation();
        Graph base = get();
        if (containsBySameTerm(addedGraph(), triple) )
            return ;
        if ( checkOnUpdate && containsBySameTerm(base, triple) )
            // Already in base graph
            return;
        addedGraph().add(triple);
    }

    private void execDelete(Triple triple) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()+".delete");
    }

    public Graph getAdded() {
        return addedGraph();
    }
}
