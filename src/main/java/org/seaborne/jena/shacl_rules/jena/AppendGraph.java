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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.system.buffering.BufferingGraph;

/*
 * An {@link AppendGraph} is a special case of a {@link BufferingGraph}.
 * <p>
 * It does not allow delete nor does it allow flushing changes back to the wrapped graph.
 */
public class AppendGraph extends BufferingGraph {

    // Long term, this could be the superclass of BufferingGraph

    public AppendGraph(Graph graph) {
        super(graph);
    }

    @Override
    public void delete(Triple t) {
        throw new UnsupportedOperationException("AppendGraph.delete(Triple) not supported");
    }

    @Override
    public void remove( Node s, Node p, Node o ) {
        throw new UnsupportedOperationException("AppendGraph.remove(Node, Node, Node) not supported");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("AppendGraph.clear() not supported");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("AppendGraph.flus() not supported");
    }

    @Override
    public void flushDirect() {
        throw new UnsupportedOperationException("AppendGraph.flushDirect() not supported");
    }

}
