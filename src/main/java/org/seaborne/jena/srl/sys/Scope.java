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

package org.seaborne.jena.srl.sys;

import java.util.*;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.seaborne.jena.srl.lang.RuleBodyElement;
import org.seaborne.jena.srl.tuples.Tuple;

/**
 *
 */
public class Scope {
    // XXX Combine with code well-formedness checking.
    // Move to parser (but rules may not be just from parsing)

    private Set<Var> inScope = new HashSet<>();

    public Scope(Set<Var> inScopeVars) {
        this.inScope = inScopeVars;
    }

    public Set<Var> inScope() {
        return this.inScope;
    }

    public static Scope scope(Set<Var> vars) {
        return new Scope(vars);
    }

    // Calculated.
    public static Scope scope(List<RuleBodyElement> elts) {
        // elts may be mutated after this call - it is used by the parser with the current partial list of body elements.

        List<Var> inScopeVars = new ArrayList<>();

        for ( RuleBodyElement elt : elts ) {
            switch(elt) {
                case RuleBodyElement.EltTriplePattern(Triple triplePattern) -> {
                    inScope(inScopeVars, triplePattern);
                }
                case RuleBodyElement.EltTuplePattern(Tuple tuplePattern) -> {
                    inScope(inScopeVars, tuplePattern);
                }

                // Only when inside
                case RuleBodyElement.EltNegation(List<RuleBodyElement> inner, boolean grounded) -> {}

                case RuleBodyElement.EltFilter(Expr condition) -> {}

                case RuleBodyElement.EltAssignment(Var var, Expr expression) -> {
                    inScopeVars.add(var);
                }
                case null -> {}
                default -> {}
            }
        }
        return new Scope(Set.copyOf(inScopeVars));
    }

    private static void inScope(Collection<Var> inScopeVars, Triple triplePattern) {
        addVar(inScopeVars, triplePattern.getSubject());
        addVar(inScopeVars, triplePattern.getPredicate());
        addVar(inScopeVars, triplePattern.getObject());

    }

    private static void addVar(Collection<Var> inScopeVars, Node node) {
        if ( Var.isNamedVar(node) )
            inScopeVars.add(Var.alloc(node));

    }

    private static void inScope(Collection<Var> inScopeVars, Tuple tuplePattern) {
        for ( int i = 0 ; i < tuplePattern.size(); i++ ) {
            addVar(inScopeVars, tuplePattern.get(i));
        }
    }

//    /**
//     * State for tracking variables.
//     * Currently, we only need to know what has been
//     * defined (set by a pattern match or an assignment).
//     */
//    private static class VarTracker {
//        // Patterns and assigned
//        final Set<Var> bodyDefined;
//        //final Set<Var> bodyMentioned;
//        //final Set<Var> headConsumed;
//
//        VarTracker() {
//            bodyDefined = new HashSet<>();
//            //bodyMentioned = new HashSet<>();
//            //headConsumed = new HashSet<>();
//        }
//
//        private VarTracker(VarTracker other) {
//            bodyDefined = new HashSet<>(other.bodyDefined);
//            //bodyMentioned = new HashSet<>(other.bodyMentioned);
//            //headConsumed = new HashSet<>(other.headConsumed);
//        }
//
//        VarTracker copyOf() {
//            return new VarTracker(this);
//        }
//    }

}
