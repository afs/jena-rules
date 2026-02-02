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

package org.seaborne.jena.shacl_rules.sys;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;

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
        for ( Triple tripleTemplate : rule.getTripleTemplates() ) {
            checkDefined(tracker, tripleTemplate.getSubject());
            checkDefined(tracker, tripleTemplate.getPredicate());
            checkDefined(tracker, tripleTemplate.getObject());
        }
    }

    private static void checkRuleElements(VarTracker tracker, List<RuleBodyElement> elts) {
        for ( RuleBodyElement elt : elts ) {
            checkRuleElement(tracker, elt);
        }
    }

    public static void checkRuleElement(VarTracker tracker, RuleBodyElement elt) {
        switch(elt) {
            case EltTriplePattern(Triple triplePattern) -> {
                addVar(tracker.bodyDefined, triplePattern.getSubject());
                addVar(tracker.bodyDefined, triplePattern.getPredicate());
                addVar(tracker.bodyDefined, triplePattern.getObject());
            }
            case EltCondition(Expr condition) -> {
                processWellFormedExpr(tracker, condition);
            }
            case EltNegation(List<RuleBodyElement> innerBody) -> {
                // Isolated tracker.
                VarTracker negTracker = tracker.copyOf();
                checkRuleElements(negTracker, innerBody);
            }
            case EltAssignment(Var var, Expr expression) -> {
                processWellFormedExpr(tracker, expression);
                if ( tracker.bodyDefined.contains(var) )
                    throw new NotWellFormedException("Assignment variable already defined: "+var);
                tracker.bodyDefined.add(var);
            }
            case null -> { throw new ShaclException("Null in rule body"); }
        }
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
    }
}

