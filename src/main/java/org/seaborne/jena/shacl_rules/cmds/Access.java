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

package org.seaborne.jena.shacl_rules.cmds;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.lang3.stream.Streams;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.main.solver.SolverRX3;
import org.seaborne.jena.shacl_rules.RuleSet;

public class Access {

    /**
     * Solve a triple patter which may
     * This function does not handled recursion.
     * This function does not handled DAGs (executes as a tree).
     */
    public static Stream<Binding> accessRuleSet(Binding input, RuleSet ruleSet, Graph graph, Triple tPattern) {


        return null;
    }

    // SolverRX3 to expose rdfStarTripleSub with a graph argument and simplified.
    public static Iterator<Binding> accessGraph(Iterator<Binding> input, Graph graph, Triple tPattern) {
        ExecutionContext execCxt = ExecutionContext.createForGraph(graph);
        // SolverRX3.rdfStarTripleSub is the flat map operation but not visible.
        Iterator<Binding> stage = SolverRX3.rdfStarTriple(input, tPattern, execCxt);
        return stage;
    }

    // Use with flatMap Function<Binding, Stream<Binding>> {}
    // XXX Encapsulate and clear up later.

    // SolverRX3 to expose rdfStarTripleSub with a graph argument and simplified.
    public static Stream<Binding> accessGraph(Binding input, Graph graph, Triple tPattern) {
        Iterator<Binding> stage = accessGraphIter(input, graph, tPattern);
        Stream<Binding> out = Streams.of(stage);
        return out;
    }

    private static Iterator<Binding> accessGraphIter(Binding input, Graph graph, Triple tPattern) {
        // When Jena 5.5.0 released.
        // SolverRX3.match(graph, input, tPattern)
        // --
        // For now, go via SolverRX3.rdfStarTriple
        ExecutionContext execCxt = ExecutionContext.createForGraph(graph);
        // SolverRX3.rdfStarTripleSub is the flat map operation but not visible.
        Iterator<Binding> in = Iter.singletonIterator(input);
        Iterator<Binding> stage = SolverRX3.rdfStarTriple(in, tPattern, execCxt);
        return stage;
    }

}
