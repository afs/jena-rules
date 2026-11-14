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
//        public NotWellFormedException(Throwable cause)                   { super(cause) ; }
//        public NotWellFormedException(String message, Throwable cause)   { super(message, cause) ; }
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

    private static void checkRuleElement(VarTracker tracker, RuleBodyElement elt, ElementLevel eltLevel) {
        switch(elt) {
            case EltTriplePattern(Triple triplePattern) -> {
                addVar(tracker.inScope, triplePattern.getSubject());
                addVar(tracker.inScope, triplePattern.getPredicate());
                addVar(tracker.inScope, triplePattern.getObject());
            }
            case EltTuplePattern(Tuple tuplePattern) -> {
                for ( int i = 0 ; i < tuplePattern.size(); i++ ) {
                    addVar(tracker.inScope, tuplePattern.get(i));
                }
            }
            case EltFilter(Expr condition) -> {
                processWellFormedExpr(tracker, condition);
            }
            case EltNegation(List<RuleBodyElement> innerBody, boolean grounded) -> {
                // Isolated tracker.
                VarTracker innerTracker = tracker.copyOf();
                checkRuleElementsLevel(innerTracker, innerBody, ElementLevel.INNER);
                // XXX inUse
            }
            case EltAssignment(Var var, Expr expression) -> {
                processWellFormedExpr(tracker, expression);
                if ( tracker.inScope.contains(var) )
                    throw new NotWellFormedException("Assignment variable already defined: "+var);
                tracker.inScope.add(var);
            }
            case EltGroup(List<Var> groupKeys, List<RuleBodyElement> aggPattern, List<SynAggregationAssignment> aggAssignments) -> {
                if ( groupKeys == null )
                    groupKeys = List.of();

                // V0 : Initial set of variables.
                // Defined group keys.

                Set<Var> x1 = new HashSet<Var>();
                groupKeys.forEach(gk->{
                    // If and only if a group key is in the input,
                    // add it to the initial variables for looking at the pattern.
                    if ( tracker.inScope.contains(gk) )
                        x1.add(gk);
                });

                VarTracker innerTracker = new VarTracker();
                innerTracker.inScope.addAll(x1);

                // Check inner
                checkRuleElementsLevel(innerTracker, aggPattern, ElementLevel.INNER);
                // XXX inUse

                // And now groupKeys must all be defined.
                if ( ! innerTracker.inScope.containsAll(groupKeys) ) {
                    // Calculate missing.
                    Set<Var> scopeMissing = new HashSet<>(groupKeys);
                    scopeMissing.removeAll(innerTracker.inScope);
                    throw new NotWellFormedException("Undefined group keys: "+scopeMissing);
                }

                // The aggregate assignment variables.
                // One aggregate can not use another aggregate's assignment variable.
                Set<Var> aggAssignVars = new HashSet<Var>();
                aggAssignments.forEach(agg->{
                    Var var = agg.aggAssignVar();
                    if ( innerTracker.inScope.contains(var) )
                        throw new NotWellFormedException("Aggregation variable already defined in pattern: "+var);
                    if ( tracker.inScope.contains(var) )
                        throw new NotWellFormedException("Aggregation variable already defined in input: "+var);
                    // add if new, error if it already exists
                    if ( ! aggAssignVars.add(var) )
                        throw new NotWellFormedException("Aggregation variable assigned twice: "+var);
                    // Checked against innerTracker
                    if ( agg.aggExpr() != null )
                        processWellFormedExpr(innerTracker, agg.aggExpr());
                });

                // Update top level tracker. Input, group keys and aggregate assignments.
                tracker.inScope.addAll(groupKeys);
                tracker.inScope.addAll(aggAssignVars);
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
            if ( ! tracker.inScope.contains(var) ) {
                throw new NotWellFormedException("Variable in rule head not defined: "+var);
            }
        }
    }

    private static void processWellFormedExpr(VarTracker tracker, Expr condition) {
        Set<Var> vars = condition.getVarsMentioned();
        for ( Var var : vars ) {
            //tracker.bodyMentioned.add(var);
            if ( ! tracker.inScope.contains(var) )
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

    private static void addVar(Set<Var> varSet, Node node) {
        if ( Var.isVar(node) )
            varSet.add(Var.alloc(node));
    }

    /**
     * State for tracking variables.
     * Currently, we only need to know what has been
     * defined (set by a pattern match or an assignment).
     */
    private static class VarTracker {
        // These are mutable.
        final Set<Var> inScope;
        final Set<Var> inUse;

        VarTracker() {
            inScope = new HashSet<>();
            inUse = new HashSet<>();

        }

        private VarTracker(VarTracker other) {
            inScope = new HashSet<>(other.inScope);
            inUse = new HashSet<>(other.inUse);
        }

        VarTracker copyOf() {
            return new VarTracker(this);
        }

        @Override
        public String toString() {
            return "VarTracker: inScope="+inScope+", inUse="+inUse;
        }
    }
}
