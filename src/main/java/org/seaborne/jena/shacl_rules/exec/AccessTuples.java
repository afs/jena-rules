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

package org.seaborne.jena.shacl_rules.exec;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.main.solver.SolverLib;
import org.seaborne.jena.shacl_rules.sys.RuleSetEvaluationCancelledException;
import org.seaborne.jena.shacl_rules.tuples.Tuple;
import org.seaborne.jena.shacl_rules.tuples.TupleStore;

/**
 * Execution of a tuple pattern.
 */
public class AccessTuples {
    //  Access.accessGraph
    public static Iterator<Binding> accessTupleStore(Iterator<Binding> chainIn, TupleStore store, Tuple pattern, RulesExecCxt ruleExecCxt) {
        return accessTupleStore(chainIn, store, pattern, null, ruleExecCxt);
    }

    private static Iterator<Binding> accessTupleStore(Iterator<Binding> input, TupleStore store, Tuple pattern,
                                                  Predicate<Tuple> filter, RulesExecCxt ruleExecCxt) {
        if ( filter != null )
            System.err.println("Predicate for accessTuples");
        if ( ! input.hasNext() )
            return Iter.nullIterator();
        return Iter.flatMap(input, binding -> {
            return accessTuple(binding, store, pattern, filter, ruleExecCxt);
        });
    }

    private static Iterator<Binding> accessTuple(Binding binding,  TupleStore store, Tuple pattern, Predicate<Tuple> filter, RulesExecCxt ruleExecCxt) {

        Tuple groundedPattern =  substituteFlat(pattern, binding) ;
        BindingBuilder resultsBuilder = Binding.builder(binding);
        Iterator<Tuple> tupleStoreIter = store.find(groundedPattern);

        Iterator<Binding> tuplesIter = Iter.mapRemove(tupleStoreIter, dataTuple -> mapper(resultsBuilder, groundedPattern, dataTuple));
        // Add cancel.
        AtomicBoolean cancelSignal = ruleExecCxt.getCancelSignal();
        Iterator<Binding> iter = tuplesIter;
        if ( cancelSignal != null ) {
            Function<Binding, Binding> cancelTest = (row)->{
                if (cancelSignal.get())
                    throw new RuleSetEvaluationCancelledException();
                return row;
            };
            iter = Iter.map(iter, cancelTest);
        }
        return iter;
    }

    private static Node tupleNode(Node node) {
        if ( node.isVariable() )
            return Node.ANY;
        return node;
    }

    private static Binding mapper(BindingBuilder resultsBuilder, Tuple pattern, Tuple data) {
        resultsBuilder.reset();
        for ( int i = 0 ; i < pattern.size() ; i++ ) {
            if ( ! insert(resultsBuilder, pattern.get(i), data.get(i) ) )
                return null;
        }
        return resultsBuilder.build();
    }

    private static boolean insert(BindingBuilder results, Node patternNode, Node dataNode) {
        if ( !Var.isVar(patternNode) )
            return true;
        Var v = Var.alloc(patternNode);
        Node x = results.get(v);
        if ( x != null )
            return SolverLib.sameTermAs(dataNode, x);
        results.add(v, dataNode);
        return true;
    }

    private static Tuple substituteFlat(Tuple pattern, Binding binding) {
        Node[] terms = new Node[pattern.size()];
        for ( int i = 0 ; i < pattern.size() ; i++ ) {
            terms[i] = substituteFlat(pattern.get(i), binding);
        }
        return Tuple.create(terms);
    }

    // Variable or not a variable.
    private static Node substituteFlat(Node n, Binding binding) {
        return Var.lookup(binding::get, n);
    }
}
