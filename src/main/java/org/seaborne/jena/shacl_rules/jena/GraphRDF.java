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

package org.seaborne.jena.shacl_rules.jena;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * An RDF graph that actively removes "non RDF" (triple which are RDF, only symmetric or generalized RDF)
 */
public class GraphRDF extends WrappedGraph {

    // replaces SafeGraph

    public GraphRDF(Graph graph) {
        super(graph);
    }

    @Override
    public ExtendedIterator<Triple> find() {
        return find(Node.ANY, Node.ANY, Node.ANY);
    }

    @Override
    public ExtendedIterator<Triple> find(Triple triple) {
        return find(triple.getSubject(), triple.getPredicate(),triple.getObject());
    }

    @Override
    public ExtendedIterator<Triple> find(Node s,Node p, Node o) {
        var iter = super.find(s,p,o);
        return iter.filterKeep(GraphRDF::isValid);
    }

    private static boolean isValid(Triple t) {
        Node s = t.getSubject();
        if ( ! s.isURI() && ! s.isBlank() )
            return false;
        Node p = t.getPredicate();
        if ( ! p.isURI() )
            return false;
        Node o = t.getPredicate();
        if ( ! o.isConcrete() )
            // No variables.
            // Beware of extensions.
            return false;

        // The 4 kinds of RDF term.
//        if ( ! o.isURI() &&
//             ! o.isBlank() &&
//             ! o.isLiteral() &&
//             ! o.isTripleTerm() )
//            return false;

        if ( ! t.getObject().isConcrete() || t.getObject().isExt() )
            // No variables. No extensions.
            return false;
        return true;
    }

    @Override
    public boolean contains(Triple triple) {
        return contains(triple.getSubject(), triple.getPredicate(),triple.getObject());
    }

    @Override
    public boolean contains(Node s,Node p, Node o) {
        var iter = find(s, p, o);
        try {
            return iter.hasNext();
        } finally { iter.close(); }
    }

    @Override
    public int size() {
        return (int)Iter.count(find());
    }

    @Override
    public boolean isEmpty() {
        return ! contains(Node.ANY, Node.ANY, Node.ANY);
    }
}
