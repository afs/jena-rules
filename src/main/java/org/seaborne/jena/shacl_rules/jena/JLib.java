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

import java.util.*;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.graph.GNode;
import org.apache.jena.sparql.util.graph.GraphList;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;

public class JLib {
    private static Node NIL = RDF.Nodes.nil;
    private static Node CAR = RDF.Nodes.first;
    private static Node CDR = RDF.Nodes.rest;
    private static Node TYPE = RDF.Nodes.type;

    // XXX----GraphList

    /**
     * Extract the elements of a list.
     */
    public static List<Node> asList(Graph graph, Node headNode) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(headNode);
        List<Triple> triples = new ArrayList<>();
        GNode gNode = GNode.create(graph, headNode);
        List<Node> x = GraphList.members(gNode);
        return x;
    }

    public static Node[] asArray(Graph graph, Node headNode) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(headNode);
        List<Triple> triples = new ArrayList<>();
        GNode gNode = GNode.create(graph, headNode);
        List<Node> x = GraphList.members(gNode);
        return x.toArray(Node[]::new);
    }

    /**
     * Build an RDF Collection (RDF list) in a graph based on the java list of nodes.
     * Return the head of the list.
     */
    public static Node addList(Graph graph, List<Node> elements) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(elements);
        ListIterator<Node> iter = elements.listIterator(elements.size());
        Node x = NIL;

        while(iter.hasPrevious()) {
            Node cell = NodeFactory.createBlankNode();
            Node elt = iter.previous();
            graph.add(cell, CAR, elt);
            graph.add(cell, CDR, x);
            x = cell;
        }
        return x;
    }

    // XXX----G

    /**
     * Return all the subjects of a predicate in a graph (no duplicates)
     * <p>
     * Use {@code iterPO(predicate, null)} for "with duplicates."
     */
    public static Set<Node> subjectsOfPredicateAsSet(Graph graph, Node predicate) {
        Objects.requireNonNull(graph, "graph");
        ExtendedIterator<Triple> iter = graph.find(Node.ANY, predicate, Node.ANY);
        return Iter.iter(iter).map(Triple::getSubject).toSet();
    }

    /**
     * Return all the the objects in a graph (no duplicates)
     * <p>
     * Use {@code iterSP(null, predicate)} for "with duplicates."
     */
    public static Set<Node> objectsOfPredicateAsSet(Graph graph, Node predicate) {
        Objects.requireNonNull(graph, "graph");
        ExtendedIterator<Triple> iter = graph.find(Node.ANY, predicate, Node.ANY);
        return Iter.iter(iter).map(Triple::getObject).toSet();
    }

    /**
     * Clone a graph - includes the prefixes.
     */
    public static Graph cloneGraph(Graph graph) {
        Graph copyGraph = GraphFactory.createGraphMem();
        GraphUtil.addInto(copyGraph, graph);
        copyGraph.getPrefixMapping().setNsPrefixes(graph.getPrefixMapping());
        return copyGraph;
    }
}
