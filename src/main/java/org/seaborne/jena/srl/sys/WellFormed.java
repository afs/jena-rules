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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleSet;
import org.seaborne.jena.srl.RulesException;
import org.seaborne.jena.srl.lang.RuleBodyElement;
import org.seaborne.jena.srl.lang.RuleBodyElement.*;
import org.seaborne.jena.srl.lang.parser.SynAggregationAssignment;
import org.seaborne.jena.srl.tuples.Tuple;

/**
 * Rule well-formed conditions.
 * Variables defined before use.
 */
public class WellFormed {

    public static class NotWellFormedException extends RulesException {
        public NotWellFormedException(String message)                    { super(message); }
//        public NotWellFormed(Throwable cause)                   { super(cause) ; }
//        public NotWellFormed(String message, Throwable cause)   { super(message, cause) ; }
    }

    /**
     * Check whether a ruleset is well-formed.
     * This function throws {@link NotWellFormedException}
     * if a problem is detected.
     */
    public static void checkWellFormed(RuleSet ruleSet) {
        for ( Rule rule : ruleSet.getRules() ) {
            checkWellFormed(rule);
        }
    }

    /**
     * Check whether a rule is well-formed.
     * This function throws {@link NotWellFormedException}
     * if a problem is detected.
     */
    public static void checkWellFormed(Rule rule) {
        VarTracker tracker = new VarTracker();

        // Body
        checkRuleElements(tracker, rule.getBodyElements());

        // Head
        for ( Triple tripleTemplate : rule.getHeadTriples() ) {
            checkDefined(tracker, tripleTemplate.getSubject());
            checkDefined(tracker, tripleTemplate.getPredicate());
            checkDefined(tracker, tripleTemplate.getObject());
        }
        for ( Tuple tupleTemplate : rule.getHeadTuples() ) {
            for ( int i = 0 ; i < tupleTemplate.size() ; i++ ) {
                checkDefined(tracker, tupleTemplate.get(i));
            }
        }
    }

    enum ElementLevel { TOP, INNER }

    // XXX better alignment the spec algorithm.
    // This is "inScope"
    // Refactor to have an "initial set" which ATM happens by careful (unclear) setup of the tracker.

    private static void checkRuleElements(VarTracker tracker, List<RuleBodyElement> elts) {
        checkRuleElementsLevel(tracker, elts, ElementLevel.TOP);
    }

    private static void checkRuleElementsLevel(VarTracker tracker, List<RuleBodyElement> elts, ElementLevel eltLevel) {
        for ( RuleBodyElement elt : elts ) {
            checkRuleElement(tracker, elt, eltLevel);
        }
    }

    public static void checkRuleElement(VarTracker tracker, RuleBodyElement elt, ElementLevel eltLevel) {
        switch(elt) {
            case EltTriplePattern(Triple triplePattern) -> {
                addVar(tracker.bodyDefined, triplePattern.getSubject());
                addVar(tracker.bodyDefined, triplePattern.getPredicate());
                addVar(tracker.bodyDefined, triplePattern.getObject());
            }
            case EltTuplePattern(Tuple tuplePattern) -> {
                for ( int i = 0 ; i < tuplePattern.size(); i++ ) {
                    addVar(tracker.bodyDefined, tuplePattern.get(i));
                }
            }
            case EltFilter(Expr condition) -> {
                processWellFormedExpr(tracker, condition);
            }
            case EltNegation(List<RuleBodyElement> innerBody, boolean grounded) -> {
                // Isolated tracker.
                VarTracker innerTracker = tracker.copyOf();
                checkRuleElementsLevel(innerTracker, innerBody, ElementLevel.INNER);
            }
            case EltAssignment(Var var, Expr expression) -> {
                processWellFormedExpr(tracker, expression);
                if ( tracker.bodyDefined.contains(var) )
                    throw new NotWellFormedException("Assignment variable already defined: "+var);
                tracker.bodyDefined.add(var);
            }
            case EltGroup(List<Var> groupKeys, List<RuleBodyElement> aggPattern, List<SynAggregationAssignment> aggAssignments) -> {

                if ( groupKeys == null )
                    groupKeys = List.of();

                // V0 : Initial set of variables.
                // XXX Improve
                // XXX Need to keep this?
                Set<Var> x1 = new HashSet<Var>();
                groupKeys.forEach(gk->{
                    if ( tracker.bodyDefined.contains(gk) )
                        x1.add(gk);
                });
                // ----
                // XXX Inner if group keys only.
                VarTracker innerTracker = new VarTracker();
                innerTracker.bodyDefined.addAll(x1);

                // Check inner
                checkRuleElementsLevel(innerTracker, aggPattern, ElementLevel.INNER);

                // And now groupKeys must all be defined.
                if ( ! innerTracker.bodyDefined.containsAll(groupKeys) ) {
                    // XXX Better message.
                    throw new NotWellFormedException("A group key may be undefined");
                }

                aggAssignments.forEach(agg->{
                    Var var = agg.aggAssignVar();
                    // var must not conflict with the grouping.
                    if ( innerTracker.bodyDefined.contains(var) )
                        throw new NotWellFormedException("Aggregation variable already defined in pattern: "+var);
                    // var must be fresh at the top level.
                    if ( tracker.bodyDefined.contains(var) )
                        throw new NotWellFormedException("Aggregation variable already defined in input: "+var);
                    if ( agg.aggExpr() != null )
                        processWellFormedExpr(innerTracker, agg.aggExpr());
                    innerTracker.bodyDefined.add(var);
                    tracker.bodyDefined.add(var);
                });
                // Update top level tracker. Input, group keys and aggregate assignments.
                if ( groupKeys != null )
                    tracker.bodyDefined.addAll(groupKeys);
                aggAssignments.forEach(agg->{
                    Var var = agg.aggAssignVar();
                    tracker.bodyDefined.add(var);
                });

            }
            case null -> { throw new RulesException("Null in rule body"); }
        }
//
//        switch(eltLevel) {
//            case TOP -> System.out.println(tracker);
//            case INNER -> System.out.println("Inner: "+tracker);
//        }
    }

    private static void checkDefined(VarTracker tracker, Node term) {
        if ( Var.isVar(term) ) {
            Var var = Var.alloc(term);
            if ( ! tracker.bodyDefined.contains(term) ) {
                throw new NotWellFormedException("Variable in rule head not defined: "+var);
            }
        }
    }

    private static void processWellFormedExpr(VarTracker tracker, Expr condition) {
        Set<Var> vars = condition.getVarsMentioned();
        for ( Var var : vars ) {
            //tracker.bodyMentioned.add(var);
            if ( ! tracker.bodyDefined.contains(var) )
                throw new NotWellFormedException("Expression variable not defined: "+var);
        }
    }

//    private static void processWellFormedGroupBy(VarTracker tracker, GropProcessor groupProc) {
//        if ( groupProc.hasGroupBy() ) {
//            for ( Var var : groupProc.groupBy() ) {
//                if ( ! tracker.bodyDefined.contains(var) )
//                    throw new NotWellFormedException("Group variable not defined: "+var);
//            }
//        }
//    }

    private static void addVar(Set<Var> bodyDefined, Node node) {
        if ( Var.isVar(node) )
            bodyDefined.add(Var.alloc(node));
    }

    /**
     * State for tracking variables.
     * Currently, we only need to know what has been
     * defined (set by a pattern match or an assignment).
     */
    private static class VarTracker {
        // Patterns and assigned
        final Set<Var> bodyDefined;
        //final Set<Var> bodyMentioned;
        //final Set<Var> headConsumed;

        VarTracker() {
            bodyDefined = new HashSet<>();
            //bodyMentioned = new HashSet<>();
            //headConsumed = new HashSet<>();
        }

        private VarTracker(VarTracker other) {
            bodyDefined = new HashSet<>(other.bodyDefined);
            //bodyMentioned = new HashSet<>(other.bodyMentioned);
            //headConsumed = new HashSet<>(other.headConsumed);
        }

        VarTracker copyOf() {
            return new VarTracker(this);
        }

        @Override
        public String toString() {
            return "Vartracker: "+bodyDefined;
        }
    }
}

