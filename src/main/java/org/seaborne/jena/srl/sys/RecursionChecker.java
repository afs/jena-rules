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

import java.util.Deque;

import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RulesException;
import org.seaborne.jena.srl.exec.RulesExecCxt;

/**
 * Checking for illegal recursion - a recursive path that goes through a negation (NOT).
 */
public class RecursionChecker {
    // Switch DependencyGraph impls.
    
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
    enum PathIncludesNegation { YES, NO_NEG }

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
        for ( Rule rule : depGraph.getRuleSet().getRules()) {
            // Throws an exception on an illegal recursion.
            /*IsRecursive isRecursive = */ RecursionChecker.checkRecursion(depGraph, rule);
        }
    }

    // Return {@code IsRecursive.YES} if safely recursive, return {@link IsRecursive.NO} if not recursive, and
    // throw exception if recursion includes a negation (illegal).
    public static IsRecursive checkRecursion(DependencyGraph depGraph, Rule rule) {
        //return RecursionChecker0.checkRecursion(depGraph, rule);

        return RecursionChecker2.checkRecursion(depGraph, rule);
    }
}
