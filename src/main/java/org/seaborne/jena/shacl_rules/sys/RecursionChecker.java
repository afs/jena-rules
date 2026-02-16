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
import org.seaborne.jena.shacl_rules.ShaclRulesWriter;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph.DependencyEdge;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph.DepEdgeType;

/**
 * Checking for illegal recursion - a recursive path that goes through a negation (NOT).
 */
public class RecursionChecker {
    // Efficiency: later:

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

    public enum IsRecursive { YES, NO }
    enum PathIncludesNegation { YES, NO }

    public static class RecursionException extends RulesException {
        private final Deque<Rule> path;

        public RecursionException(String msg, Deque<Rule> path) {
            super("RecursionException");
            this.path = path;
        }
        public Deque<Rule> getPath() { return path; }
    }

    /*
     * Check for illegal recursion - a recursive path that goes through a negation (NOT).
     * This function throws an exception if it finds an illegal recursion.
     */
    public static void checkForIllegalRecursion(DependencyGraph depGraph) {
        for ( Rule rule : depGraph.getRuleSet().getRules()) {
            // Throws an exception on an illegal recursion.
            /*IsRecursive isRecursive = */RecursionChecker.checkRecursion(depGraph, rule);
        }
    }

    // Return {@code IsRecursive.YES} if safely recursive, return {@link IsRecurive.NO} if not recursive, and
    // throw exception if recursion includes a negation (illegal).
    public static IsRecursive checkRecursion(DependencyGraph depGraph, Rule rule) {
        Deque<Rule> path = new ArrayDeque<>();
        IsRecursive isRecursive = RecursionChecker.checkRecursion(depGraph, rule, PathIncludesNegation.NO, rule, path);
        return isRecursive;
    }

    private static IsRecursive checkRecursion(DependencyGraph depGraph, Rule topRule, PathIncludesNegation seenNegation, Rule rule, Deque<Rule> path) {
        if ( path.contains(rule) ) {
            if ( seenNegation == PathIncludesNegation.YES ) {
                String ruleStr = ShaclRulesWriter.asString(rule, null);
                throw new RecursionException(ruleStr, path);
            }
            return IsRecursive.YES;
        }
        path.push(rule);
        IsRecursive isRecursive = RecursionChecker.checkRecursionStep(depGraph, topRule, seenNegation, rule, path) ;
        path.pop();
        return isRecursive;
    }

    // topRule is the overall rule we are testing. */
    private static IsRecursive checkRecursionStep(DependencyGraph depGraph, Rule topRule, PathIncludesNegation seenNegation, Rule visitRule, Deque<Rule> visited) {
        Collection<DependencyEdge> providedBy = depGraph.directDependencies(visitRule);
        if ( providedBy.isEmpty() )
            return IsRecursive.NO;

        boolean recursion = false;
        for( DependencyEdge edge : providedBy ) {
            PathIncludesNegation seen = seenNegation;
            if ( edge.link() == DepEdgeType.NEGATIVE )
                seen = PathIncludesNegation.YES;
            IsRecursive stepIsRecursive = checkRecursion(depGraph, topRule, seen, edge.linkedRule(), visited);
            if ( stepIsRecursive == IsRecursive.YES )
                recursion = true;
        }
        return recursion ? IsRecursive.YES : IsRecursive.NO;
    }
}
