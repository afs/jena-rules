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

package org.seaborne.jena.shacl_rules.sys;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.exec.RulesExecCxt;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph.DependencyEdge;
import org.seaborne.jena.shacl_rules.sys.RecursionChecker.IsRecursive;

/**
 * Checking for illegal recursion - a recursive path that goes through a negation (NOT).
 */
public class RecursionChecker2 {
    // Efficiency: later:

    // Look for recursion fo a rule.
    // Is the rule negated?
    // --> error.

    // XXX Only need to test rules with negation rules

    // Use Strongly connected components.
    //   which may also help with evaluation (do cycles differently to non-cycles)
    //
    // Path-based strong component algorithm
    // https://en.wikipedia.org/wiki/Path-based_strong_component_algorithm
    //
    // Tarjan's algorithm
    // https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
    //
    // Kosaraju's algorithm
    // https://en.wikipedia.org/wiki/Kosaraju%27s_algorithm
    // ("simpler, more expensive")

    // Handle shared DAGs by caching at the ruleset level.

    public static class RecursionException extends RulesException {
        private final Deque<Rule> path;

        private RecursionException(String msg, Deque<Rule> path) {
            super(msg);
            this.path = path;
        }
        public Deque<Rule> getPath() { return path; }
    }

    public static void checkForIllegalRecursion(DependencyGraph depGraph) {
        checkForIllegalRecursion(depGraph, RulesExecCxt.get());
    }

    /*
     * Check for illegal recursion - a recursive path that goes through a negation (NOT).
     * This function throws an exception if it finds an illegal recursion.
     */
    public static void checkForIllegalRecursion(DependencyGraph depGraph, RulesExecCxt rCxt) {
        // XXX Change to check only rules which requite strict stratification
        // Rule with a negation or it is run-once.

        if ( ! SysJenaRules.performRecursionCheck )
            return;
        for ( Rule rule : depGraph.getRuleSet().getRules()) {

            // XXX Add this test
            //if ( rule.isRunOnceRule() || rule.hasNegation() )

            // Throws an exception on an illegal recursion.
            /*IsRecursive isRecursive = */ RecursionChecker.checkRecursion(depGraph, rule);
        }
    }


    // Return {@code IsRecursive.YES} if safely recursive, return {@link IsRecursive.NO} if not recursive, and
    // throw exception if recursion includes a negation (illegal).
    public static IsRecursive checkRecursion(DependencyGraph depGraph, Rule rule) {
        Deque<Rule> visited = new ArrayDeque<>(); // **LinkHashSet:add/remove
        boolean isRecursive = ruleIsRecursive(depGraph, rule, rule, visited);   //Start.
        //if ( isRecursive && rule.isGrounded() ) {}

        if ( isRecursive && rule.hasNegation() )
            throw new RecursionException("Recursion failure", new ArrayDeque<>());
        return isRecursive? IsRecursive.YES : IsRecursive.NO ;
    }

    // A rule is recursive if a walk visits the start node.
    // NOT is there is a loop somewhere but not including the start.

    private static boolean ruleIsRecursive(DependencyGraph depGraph, Rule topRule, Rule visitRule, Deque<Rule> visited) {
        visited.push(visitRule);
        // Go down a level.
        boolean visitsTop = walk(depGraph, topRule, visitRule, visited);
        visited.pop();
        return visitsTop;
    }

    private static boolean walk(DependencyGraph depGraph, Rule topRule, Rule visitRule, Deque<Rule> visited) {
        Collection<DependencyEdge> providedBy = depGraph.directDependencies(visitRule);
        if ( providedBy.isEmpty() ) {
            return false;
        }

        // Dev tools: DUMP

        // Go across a level (depth first search).
        for( DependencyEdge edge : providedBy ) {
            // Walk
            Rule next = edge.linkedRule();
            if ( next == topRule )
                return true;
            // Inner loop: don't loop forever.
            if ( haveVisited(visited, next) ) {
                continue;
            }
            visited.push(next);
            boolean visitsTop = ruleIsRecursive(depGraph, topRule, next, visited);
            visited.pop();
            if ( visitsTop )
                return true;
        }
        return false;
    }

    private static <X> boolean haveVisited(Collection<X> elts, X x) {
        for ( X e : elts ) {
            if ( e == x )
                return true;
        }
        return false;
    }
}
