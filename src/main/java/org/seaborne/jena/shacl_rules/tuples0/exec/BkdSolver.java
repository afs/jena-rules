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

package org.seaborne.jena.shacl_rules.tuples0.exec;

class BkdSolver{}
//
//import static java.lang.String.format;
//
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.function.Function;
//
//import org.apache.jena.sparql.engine.binding.Binding;
//import org.apache.jena.sparql.engine.binding.BindingBuilder;
//import org.apache.jena.sparql.engine.binding.BindingFactory;
//import org.seaborne.rules.*;
//import org.seaborne.rules.exec.MGU;
//import org.seaborne.rules.exec.RuleOps;
//import org.apache.jena.atlas.iterator.Iter;
//import org.apache.jena.atlas.lib.InternalErrorException;
//import org.apache.jena.atlas.lib.tuple.Tuple;
//import org.apache.jena.graph.Node;
//import org.apache.jena.sparql.core.Var;
//
//public class BkdSolver {
//    // Solving.
//    // To match (a b c) we need to look in the data to if there are direct matches,
//    // and also see if there are any rules that can be used to produce matches.
//    //
//    // Crude - does not handle:
//    //   Left recursion.
//    //   Shared rules
//
//    /**
//     * For a non-recursive ruleset, rewrite pattern to become an expression of {@link Rel Rels} that are database accesses.
//     */
//    public static void expand(Rel pattern, RuleSet ruleSet) {
//        // Should be a conjunction-disjunction tree
//        // Rename variables.
//    }
//
//    /**
//     * Solve a single rel pattern.
//     * Produces bindings for the pattern.
//     * Data is on argument {@code relStore}, rules in {@code ruleSet}.
//     */
//    public static Iterator<Binding> solver(Rel pattern, RuleSet ruleSet, RelStore relStore) {
//        // Get the version of the rules that have unique variable names.
//
//        RuleExecCxt rCxt = RuleExecCxt.global;
//        if ( rCxt.trace() )
//            rCxt.out().printf("solver(%s)\n", pattern);
//
//        // XXX Check for recursion.
//        //ruleSet.isRecursive();
//        try {
//            Iterator<Binding> iter = solver(rCxt, 1, BindingFactory.root(), pattern, ruleSet, relStore);
//            if ( rCxt.trace() )
//                rCxt.out().println();
//            //iter = RulesLib.print(rCxt.out(), iter);
//            return iter;
//        } finally { rCxt.out().flush(); }
//    }
//
//    /**
//     * The main solver - this is called recursively.
//     */
//    private static Iterator<Binding> solver(RuleExecCxt rCxt, int depth, Binding input, Rel pattern, RuleSet ruleSet, RelStore relStore) {
//        functionEnter(rCxt, "solver[%s](%s, %s)", depth, pattern, input);
//        Iterator<Binding> iter = solverWorker(rCxt, depth, input, pattern, ruleSet, relStore);
//        iter = functionExit(rCxt, iter, "solver[%d](%s, %s)", depth, pattern, input);
//        return iter;
//    }
//
//    private static Iterator<Binding> solverWorker(RuleExecCxt rCxt, int depth, Binding input, Rel pattern, RuleSet ruleSet, RelStore relStore) {
//        Rel pattern1 = Sub.substitute(input, pattern);
//        // Non-recursive => at most one deep for each rule in the ruleset
//        // Max depth is RuleSet.size
//
//        if ( depth > ruleSet.size()+1 )
//            throw new RulesException(format("Too deep: depth = %d, RuleSet size = %d", depth, ruleSet.size()));
//
//        // Data/EDB
//        Iterator<Binding> matches1 = dataSolver(rCxt, input, pattern1, relStore);
//        // Pass across?
//        Iterator<Binding> matches2 = ruleSetSolver(rCxt, depth, input, pattern1, ruleSet, relStore);
//
//        Iterator<Binding> matches = Iter.concat(matches1, matches2);
//        if ( matches == null )
//            return Iter.nullIterator();
//        return matches;
//    }
//
//    /** Find matching rules and resolve. */
//    private static Iterator<Binding> ruleSetSolver(RuleExecCxt rCxt, int depth, Binding input, Rel pattern1, RuleSet ruleSet, RelStore relStore) {
//        functionEnter(rCxt, "ruleSetSolver(%s)", pattern1);
//        Iterator<Binding> iter = ruleSetSolverWorker(rCxt, depth, input, pattern1, ruleSet, relStore);
//        iter = functionExit(rCxt, iter, "ruleSetSolver(%s)", pattern1);
//        return iter;
//    }
//
//    private static Iterator<Binding> ruleSetSolverWorker(RuleExecCxt rCxt, int depth, Binding input, Rel pattern1, RuleSet ruleSet, RelStore relStore) {
//        Collection<Rule> matchingRules = findCompatible(ruleSet, pattern1);
//        if ( matchingRules.isEmpty() ) {
//            return Iter.nullIterator();
//        }
//
//        Iterator<Binding> chain = null;
//        for ( Rule rule: matchingRules ) {
//            // XXX Is this another flatMap?
//            //chain = Iter.flatMap(chain, binding->solver(rCxt, depth+1, binding, relBody, ruleSet, relStore));
//            Iterator<Binding> chainStep = ruleEvalSolver(rCxt, depth, input, pattern1, rule, ruleSet, relStore);
//            chain = Iter.concat(chain,  chainStep);
//        }
//        return chain;
//    }
//
//    /**
//     * Find matching rules: A rule is compatible if it would contribute to the pattern.
//     * Uses the rewritten list of rules so variable names are unique across rules.
//     * Does not consider pattern of variables (e.g. "(?x ?x)").
//     */
//    private static Collection<Rule> findCompatible(RuleSet ruleSet, Rel pattern) {
//        return RuleOps.provides(pattern, ruleSet.execRules());
//    }
//
//    /** Resolve one rule. */
//    private static Iterator<Binding> ruleEvalSolver(RuleExecCxt rCxt, int depth, Binding input, Rel pattern, Rule rule, RuleSet ruleSet, RelStore relStore) {
//        functionEnter(rCxt, "ruleSolver(%s, %s, %s)", pattern, rule, input);
//        //rCxt.out().flush();
//        Iterator<Binding> iter = ruleEvalWorker(rCxt, depth, input, pattern, rule, ruleSet, relStore);
//        iter = functionExit(rCxt, iter, "ruleSolver(%s, %s, %s)", pattern, rule, input);
//        return iter;
//    }
//
//    private static Iterator<Binding> ruleEvalWorker(RuleExecCxt rCxt, int depth, Binding input, Rel pattern0, Rule rule, RuleSet ruleSet, RelStore relStore) {
//        // Ground the query
//        Rel pattern1 = Sub.substitute(input, pattern0);
//        // Mapping to rule vars (assumes no variable name clash - a varaible in the
//        // rule bode, not in the head, that is teh same name as in the query.
//
//        Binding mgu = MGU.mgu(pattern1, rule.getHead());
//        if ( mgu == null )
//            return null;
//        Rel pattern2 = MGU.applyMGU(mgu, pattern1);
//
//        Iterator<Binding> chain = Iter.singletonIterator(BindingFactory.root());
//
//        for ( Rel relBody_ : rule.getBody() ) {
//            Rel relBody = Sub.substitute(mgu, relBody_);
//            chain = Iter.flatMap(chain, binding->solver(rCxt, depth+1, binding, relBody, ruleSet, relStore));
//            if ( rCxt.trace() )
//                chain = Iter.materialize(chain);
//        }
//
//        // Now covert the rule body outcome to the variables of the pattern.
//        // XXX This needs cleaner code.
//
//        Function<Var,Node> headProject = mapRelFromTo(rCxt, rule.getHead(), pattern1);
//        Function<Binding, Binding> resultMapper = b1 -> {
//            BindingBuilder builder = Binding.builder(b1);
//            Iterator<Var> vIter = mgu.vars();
//            for (; vIter.hasNext(); ) {
//                Var v = vIter.next();
//                Node n = mgu.get(v);
//                // XXX Clash? Implies non-unique naming?
//                if ( ! b1.contains(v) )
//                    builder.add(v,n);
//            }
//            Binding b2 = builder.build();
//            return b2;
//        };
//
//        chain = Iter.iter(chain).map(resultMapper);
//        return chain;
//    }
//
//    /**
//     * For two {@link #compatible(Rel,Rel)} rels,
//     * create mapping from the first arg to the second arg.
//     * This is also a projection (only dstRel vars will show up).
//     * Returns null on "no mapping".
//     * @param mgu
//     */
//    private static Function<Var, Node> mapRelFromTo(RuleExecCxt rCxt, Rel srcRel, Rel dstRel) {
//        // XXX Testable!
//        // XXX Overkill : by number of slots version.
//        Map<Var, Node> mapping = new HashMap<>();
//        if ( srcRel.len() != srcRel.len())
//            throw new InternalErrorException(format("mapRelFromTo: %d / %d", srcRel.len() != dstRel.len()));
//        int N = srcRel.len();
//        //map(srcRel)
//
//        for ( int i = 0 ; i<N ; i++ ) {
//            Node src = srcRel.getTuple().get(i);
//            Node dst = dstRel.getTuple().get(i);
//            if ( Var.isVar(src) && Var.isVar(dst) ) {
//                mapping.put(Var.alloc(src), dst);
//                continue;
//            }
//            if ( ! Var.isVar(src) && Var.isVar(dst) ) {
//                mapping.put(Var.alloc(dst), src);
//            }
//        }
//        return v -> mapping.getOrDefault(v, null);
//    }
//
//    /**
//     * Determine if the pattern (which may itself have variables) might match the rule head.
//     * That is, any slots where each has a constant, match?
//     * No consideration of variable names.
//     * @param patten
//     * @param data
//     * @return
//     */
//    private static boolean compatible(Rule rule, Rel pattern) {
//        Rel targetRel = rule.getHead();
//        Rel srcRel = pattern;
//        if( targetRel.len() != srcRel.len() )
//            return false;
//        if( ! targetRel.getName().equals(srcRel.getName()) )
//            return false;
//
////        return compatible(rule.getHead(), pattern);
////    }
////
////    /**
////     * Rel compatibility.
////     * <ul>
////     * <li> Ary-agrees
////     * <li> Do const-const match?
////     * </ul>
////     */
////    private static boolean compatible(Rel targetRel, Rel srcRel) {
//        // To match:
//        //   Do const-const match?
//        //   Can we bind a data pattern to a rule const?
//        //   Can we ground the rule use a const of the data?
//
//        //return compatible(head, rel);
//        Tuple<Node> tuple1 = targetRel.getTuple();
//        Tuple<Node> tuple2 = srcRel.getTuple();
//
//        if( tuple1.len() != tuple2.len() )
//            return false;
//        // Const-const match.
//        for ( int i = 0 ; i < tuple1.len(); i++ ) {
//            Node n1 = tuple1.get(i);
//            Node n2 = tuple2.get(i);
//            if ( ! constCompatible(n1,n2) )
//                return false;
//        }
//
//        return true;
//    }
//
//    private static boolean constCompatible(Node n1 /*rule*/, Node n2 /*pattern*/) {
//        if ( Var.isVar(n1) || Var.isVar(n2) )
//            return true;
//        // Both ground - must match.
//        return n1.equals(n2);
//    }
//
//    /** Evaluate a pattern against ground data in a {@link RelStore}. */
//    public static Iterator<Binding> dataSolver(RuleExecCxt rCxt, Binding input, Rel pattern, RelStore relStore) {
//        functionEnter(rCxt, "dataSolver(%s, %s)", pattern, input);
//
//        Iterator<Rel> iter1 = relStore.find(pattern);
//        Iterator<Binding> iter2 = RuleOps.bindings(iter1, pattern);
//
//        iter2 = functionExit(rCxt, iter2, "dataSolver(%s, %s)", pattern, input);
//        return iter2;
//    }
//
//    private static void functionEnter(RuleExecCxt rCxt, String fmt, Object...args) {
//        if ( rCxt.trace() ) {
//            rCxt.out().printf("> "+fmt, args);
//            if ( !fmt.endsWith("\n") )
//                rCxt.out().println();
//            rCxt.out().incIndent();
//        }
//    }
//
//    private static <X> Iterator<X> functionExit(RuleExecCxt rCxt, Iterator<X> iter, String fmt, Object...args) {
//        if ( rCxt.trace() ) {
//            rCxt.out().decIndent();
//            rCxt.out().printf("< "+fmt, args);
//            if ( !fmt.endsWith("\n") )
//                rCxt.out().println();
//            iter = RuleOps.print(rCxt.out(), "  - ", iter);
//        }
//        return iter;
//    }
//}
