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
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltNegation;

/**
 * Rule well-formed conditions.
 * Variables defined before use.
 */
public class WellFormed {

    public static class NotWellFormed extends RulesException {
        public NotWellFormed(String message)                    { super(message); }
//        public NotWellFormed(Throwable cause)                   { super(cause) ; }
//        public NotWellFormed(String message, Throwable cause)   { super(message, cause) ; }
    }

    public static boolean wellFormed(Rule rule) {
        // head
        // body

        VarTracker tracker = new VarTracker();

        for ( RuleElement elt : rule.getBodyElements() ) {
            switch(elt) {
                case RuleElement.EltTriplePattern(Triple triplePattern) -> {
                    addVar(tracker.bodyDefined, triplePattern.getSubject());
                    addVar(tracker.bodyDefined, triplePattern.getPredicate());
                    addVar(tracker.bodyDefined, triplePattern.getObject());
                }
                case RuleElement.EltCondition(Expr condition) -> {
                    processWellFormedExpr(tracker, condition);
                }
                case EltNegation(List<RuleElement> innerBody) -> {
                    ElementGroup innerGroup = RuleLib.ruleEltsToElementGroup(innerBody);
                    Expr expression = new E_NotExists(innerGroup);
                    // XXX ???
                    processWellFormedExpr(tracker, expression);
                }
                case RuleElement.EltAssignment(Var var, Expr expression) -> {
                    processWellFormedExpr(tracker, expression);
                    if ( tracker.bodyDefined.contains(var) )
                        throw new NotWellFormed("Assignment variable already defined: "+var);
                    tracker.bodyDefined.add(var);
                }
                case null -> { throw new ShaclException("Null in rule body"); }
            }
        }

        // triple.forEach

        for ( Triple tripleTemplate : rule.getTripleTemplates() ) {
            checkDefined(tracker, tripleTemplate.getSubject());
            checkDefined(tracker, tripleTemplate.getPredicate());
            checkDefined(tracker, tripleTemplate.getObject());
        }

        return true;
    }

    private static void checkDefined(VarTracker tracker, Node term) {
        if ( Var.isVar(term) ) {
            Var var = Var.alloc(term);
            if ( ! tracker.bodyDefined.contains(term) ) {
                throw new NotWellFormed("Variable in rule head not defined: "+var);
            }
        }
    }

    private static void processWellFormedExpr(VarTracker tracker, Expr condition) {
        Set<Var> vars = condition.getVarsMentioned();
        for ( Var var : vars ) {
            tracker.bodyMentioned.add(var);
            if ( ! tracker.bodyDefined.contains(var) )
                throw new NotWellFormed("Expression variable not defined: "+var);
        }
    }

    private static void addVar(Set<Var> bodyDefined, Node node) {
        if ( Var.isVar(node) )
            bodyDefined.add(Var.alloc(node));
    }

    private static class VarTracker {
        // Patterns and assigned
        Set<Var> bodyDefined = new HashSet<>();
        Set<Var> bodyMentioned = new HashSet<>();
        Set<Var> headConsumed = new HashSet<>();

        VarTracker() {}

    }

}
