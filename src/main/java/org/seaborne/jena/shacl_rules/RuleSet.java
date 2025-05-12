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

package org.seaborne.jena.shacl_rules;

import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.graph.GraphFactory;

public class RuleSet {

    private final List<Rule> rules;
    private final Prologue prologue;
    private final List<Triple> dataTriples;
    private final Graph data;

    public RuleSet(Prologue prologue, List<Rule> rules, List<Triple> dataTriples) {
        this.prologue = prologue;
        this.rules = rules;
        this.dataTriples = dataTriples;
        Graph graph = null;
        if ( dataTriples != null && ! dataTriples.isEmpty() ) {
            graph = GraphFactory.createDefaultGraph();
            GraphUtil.add(graph, dataTriples);
        }
        this.data = graph;
    }

    public Prologue getPrologue() {
        return prologue;
    }

    public boolean hasPrologue() {
        if ( prologue == null )
            return false;
        return prologue.getBase() != null || !prologue.getPrefixMapping().hasNoMappings();
    }

    public List<Rule> getRules() {
        return rules;
    }

    public Graph getData() {
        return data;
    }

    public List<Triple> getDataTriples() {
        return dataTriples;
    }

    public boolean hasData() {
        if ( dataTriples == null )
            return false;
        return ! dataTriples.isEmpty();
    }

    @Override
    public String toString() {
        return rules.toString();
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }
}
