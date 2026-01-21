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
import org.seaborne.jena.shacl_rules.sys.DependencyGraph.Edge;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph.Link;

/**
 * Checking for illegal recursion - a recursive path that goes through a negation (NOT).
 */
public class RecursionChecker {

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

    private final static boolean DEBUG_RECURSIVE = false;

    /*
     * Check for illegal recursion - a recursive path that goes through a negation (NOT).
     * This function throws an exception if it finds an illegal recursion.
     */
    public static void checkForIllegalRecursion(DependencyGraph depGraph) {
        System.out.println("== Check recursion");
        for ( Rule rule : depGraph.getRuleSet().getRules()) {
            ShaclRulesWriter.print(rule, depGraph.getRuleSet().getPrefixMap());
            try {
                IsRecursive isRecursive = RecursionChecker.checkRecursion(depGraph, rule);
                switch (isRecursive) {
                    case NO->{ System.out.println("--> No recursion"); }
                    case YES-> { System.out.println("--> Safe recursion"); }
                    //null ->{}
                }
            } catch ( RecursionException ex ) {
                System.out.println("--> Error: "+ex.getMessage());
                System.out.println("Path");
                int i = 0 ;
                for ( Rule pathRule : ex.getPath().reversed() ) {
                    i++;
                    System.out.printf("%d: ", i);
                    ShaclRulesWriter.print(pathRule);
                }
                if ( false ) {
                    // Debug.
                    try {
                        RecursionChecker.checkRecursion(depGraph, rule);
                    } catch ( RecursionException ex2 ) {}
                }

            }
        }
        System.out.println();
    }



    // Return true if safely recursive, false if not recursive, and exception if recursion includes a negation.
    // Later - handle shared DAGs by caching at the ruleset level.
    public static IsRecursive checkRecursion( DependencyGraph depGraph, Rule rule) {
        Deque<Rule> path = new ArrayDeque<>();
        //Set<Rule> visited = new HashSet<>();
        IsRecursive isRecursive = RecursionChecker.checkRecursion(depGraph, rule, PathIncludesNegation.NO, rule, path);
//            if ( IsRecursive.YES == isRecursive ) {
//                if ( DEBUG_RECURSIVE ) {
//                    stack.stream().map(r->r.getTripleTemplates()).forEach(h->System.out.printf("--%s", h));
//                    System.out.println();
//                    System.out.println(stack);
//                }
//            }
        return isRecursive;
    }

    private static IsRecursive checkRecursion(DependencyGraph depGraph, Rule topRule, PathIncludesNegation seenNegation, Rule rule, Deque<Rule> path) {
        if ( DEBUG_RECURSIVE ) System.out.printf("isRecursive(\n  %s,\n  %s,\n  %s)\n", topRule, rule, path);
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
        Collection<Edge> providedBy = depGraph.directDependencies(visitRule);
        if ( providedBy.isEmpty() )
            return IsRecursive.NO;

        boolean recursion = false;
        for( Edge edge : providedBy ) {
            PathIncludesNegation seen = seenNegation;
            if ( edge.link() == Link.NEGATIVE )
                seen = PathIncludesNegation.YES;
            IsRecursive stepIsRecursive = checkRecursion(depGraph, topRule, seen, edge.linkedRule(), visited);
            if ( stepIsRecursive == IsRecursive.YES )
                recursion = true;
        }
        return recursion ? IsRecursive.YES : IsRecursive.NO;
    }
}