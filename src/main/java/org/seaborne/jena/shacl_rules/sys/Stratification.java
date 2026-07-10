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

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiMapUtils;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.ShaclRulesWriter;
import org.seaborne.jena.shacl_rules.exec.RulesExecCxt;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph.DependencyEdge;

/**
 * A stratification of a rule set.
 */
public class Stratification {
    // See also RecursionChecker
    // Stratification that the recursion check has been done.
    // The code is defensive against a recursion-negation but does not yield a
    // stratification.

    // Later - combine with "Connected components" and SCCs

    public static class StratificationException extends RulesException {
        public StratificationException(String message) {
            super(message);
        }
    }

    final private static boolean TRACE = false;
    final private int minStratum;
    final private int maxStratum;
    final private List<Stratum> stratumLevels;

    // Setting used to have data rules (no dependencies)
    // separate rules with rule dependencies in level 0.
    static Integer dataStratum = Integer.valueOf(0);
    // Setting uses to have separate data rules (no dependencies)
    // from rule with rule dependencies.
    static Integer minDependentStratum = Integer.valueOf(1);

    public static Stratification create(RuleSet ruleSet) throws StratificationException {
        RulesExecCxt rCxt = RulesExecCxt.get();
        DependencyGraph depGraph = DependencyGraph.create(ruleSet, rCxt);
        return create(ruleSet, depGraph, rCxt);
    }

    public static Stratification create(RuleSet ruleSet, DependencyGraph depGraph) throws StratificationException {
        return functionCreateStratification(ruleSet, depGraph, RulesExecCxt.get());
    }

    public static Stratification create(RuleSet ruleSet, DependencyGraph depGraph, RulesExecCxt rCxt) throws StratificationException {
        return functionCreateStratification(ruleSet, depGraph, rCxt);
    }

    private Stratification(int minStratum, int maxStratum,  List<Stratum> stratumLevels, RuleSet ruleSet) {
        if ( minStratum < 0 )
            throw new IllegalArgumentException("Negative minStratum");
        this.minStratum = minStratum;
        this.maxStratum = maxStratum;
        this.stratumLevels = stratumLevels;
    }

    public Stratum getLevel(int i) {
        return stratumLevels.get(i - minStratum);
    }

    public Stratum getLevel(Integer i) {
        return stratumLevels.get(i - minStratum);
    }

    public int minStratum() {
        return minStratum;
    }

    public int maxStratum() {
        return maxStratum;
    }

    /**
     * Apply an action to each (stratum, rule) pair.
     */
    public void forEach(BiConsumer<Integer, Stratum> action) {
        for ( int i = 0 ; i < stratumLevels.size() ; i++ ) {
            Stratum stratum = stratumLevels.get(i);
            action.accept(i, stratum);
        }
    }

    // ----

    private static Stratification functionCreateStratification(RuleSet ruleSet, DependencyGraph depGraph, RulesExecCxt rCxt) throws StratificationException {
        List<Rule> rules = ruleSet.getRules();

        if ( rules.isEmpty() )
            return new Stratification(0, -1, List.of(), ruleSet);

        Map<Rule, Integer> stratumMap = new HashMap<>();
        int minStratum = dataStratum;
        int maxStratum = 0;

        // ---- StratumMap
        // Step 1, for each rule, calculate it's stratum

        // The data layer is rules that only depend on the data and so have no edges in the dependency graph.
        // Initialize all rules to stratum data or lowest with-dependencies stratum

        boolean seenDataLayer = false;

        for ( Rule rule : rules ) {
            if ( depGraph.isDataRule(rule) )
                seenDataLayer = true;
            Integer initValue = depGraph.isDataRule(rule) ? dataStratum : minDependentStratum;
            stratumMap.put(rule, initValue);
        }

//        // The data layer may be empty.
//        if ( ! seenDataLayer ) {
//            minStratum = minDependentStratum;
//            // And reduce maxStratum.
//        }

        // We do need the strata to start at zero because of storing in a Java list.
        // We could move everything down!
        // For now, have a potential empty 0-stratum.

        if ( TRACE ) {
            stratumMap.forEach((rule, integer) -> {
                System.out.printf("StratumMap input %d : %s\n", integer, rule);
            });
            if ( ! stratumMap.isEmpty() )
                System.out.println();
        }

        // Bad recursion would cause this to go on forever.
        boolean changed = true;

        // This is an upper bound on the number of strata.
        // Only the dataStraum can have zero rules.
        // Otherwise, each stratum has at least one rule and level numbering has no gaps.
        // So if every stratum is filled with one rule, there would be this limit number of strata.
        // (+1 is for the data stratum)
        final int limit = rules.size()+1;

        final boolean TRACE_RULE_LOOP = false && TRACE; // Development only - verbose

        while(changed) {
            changed = false;
            if ( TRACE_RULE_LOOP )
                System.out.println();
            for ( DependencyEdge e : depGraph.edges() ) {
                // Edge from p to q of type sign
                Rule pRule = e.rule();
                Rule qRule = e.linkedRule();

                if ( false && TRACE_RULE_LOOP ) {
                    System.out.println("pRule: "+ruleSet.labelFor(pRule)+" :: "+pRule);
                    System.out.println("qRule: "+ruleSet.labelFor(qRule)+" :: "+qRule);
                }

                switch(e.link()) {
                    case OPEN -> {
                        if ( stratumMap.get(pRule) < stratumMap.get(qRule) ) {
                            stratumMap.put(pRule, stratumMap.get(qRule));
                            changed = true;
                        }
                    }
                    case CLOSED -> {
                        if ( stratumMap.get(pRule) <= stratumMap.get(qRule) ) {
                            int xStratum = 1 + stratumMap.get(qRule);
                            if ( xStratum > limit )
                                throw new StratificationException("Stratification error");
                            stratumMap.put(pRule, xStratum);
                            changed = true;
                        }
                    }
                    // case AGGREGATE->{}
                    default -> {
                        throw new StratificationException("Stratification error: unknowmn link type: " + e.link());
                    }
                }
            }
        }

        if ( TRACE ) {
            stratumMap.forEach((rule, integer) -> {
                System.out.printf("StratumMap layer %d : %s\n", integer, rule);
            });
            if ( ! stratumMap.isEmpty() )
                System.out.println();
        }

        // ---- Levels : Collect rules into strata.
        // -- Divide into runOnce and runAll, then setup the layers list.

        ListValuedMap<Integer, Rule> stratumRunOnce = MultiMapUtils.newListValuedHashMap();
        ListValuedMap<Integer, Rule> stratumRunGeneral = MultiMapUtils.newListValuedHashMap();

        for ( Entry<Rule, Integer> entry : stratumMap.entrySet() ) {
            Rule rule = entry.getKey();
            Integer stratumNum = entry.getValue();
            if ( rule.isRunOnceRule() ) {
                // stratumRunOnce.put(stratumNum, rule);
                // Is it permitted for unsafe evaluation?
                // If it is run-once because of assignment but does not have
                // blank node templates, then run as a general rule.
                // Similarly, if run-once because blank node templates, but not
                // assignments, then run as a general rule.

                // XXX Better way?
                boolean allowAssigmentOnly = SysJenaRules.allowUnsafeAssigments && rule.hasAssignment() && !rule.hasTemplateBlankNodes();
                boolean allowBlankNodeTemplatesOnly = SysJenaRules.allowUnsafeAssigments && rule.hasTemplateBlankNodes() && !rule.hasAssignment();
                boolean allowBoth = SysJenaRules.allowUnsafeAssigments && SysJenaRules.allowUnsafeTemplates;

                if ( allowAssigmentOnly || allowBlankNodeTemplatesOnly || allowBoth )
                    stratumRunGeneral.put(stratumNum, rule);
                else
                    // run-once
                    stratumRunOnce.put(stratumNum, rule);

            } else {
                stratumRunGeneral.put(stratumNum, rule);
            }
            maxStratum = Math.max(maxStratum, stratumNum);
        }

        List<Stratum> layers = new ArrayList<>(maxStratum);
        for ( int i = minStratum ; i <= maxStratum ; i++ ) {
            // ListValuedMap.get(i) returns an empty collection if there is no such key.
            Stratum stratum = new Stratum(stratumRunOnce.get(i), stratumRunGeneral.get(i));
            // Looses minStratum, maxStratum.
            layers.add(stratum);
        }

        if ( TRACE ) {
            // Development.
            System.out.println();
            System.out.printf("==== Strata (max = %d)\n", maxStratum);
            for ( int i = 0 ; i <= maxStratum ; i++ ) {
                System.out.printf("== Layer %d\n", i);
                Stratum layer = layers.get(i);

                Collection<Rule> stratumOnce = layer.runOnce();
                Collection<Rule> stratumAll = layer.runGeneral();

                if ( stratumOnce.isEmpty() && stratumAll.isEmpty() ) {
                    System.err.printf("No rules at level %d\n", i);
                    continue;
                }

                System.out.printf("Level %d\n", i);
                stratumOnce.forEach(rule-> {
                    System.out.print("  ");
                    ShaclRulesWriter.print(rule, ruleSet.getPrefixMap());
                });
                stratumAll.forEach(rule-> {
                    System.out.print("  ");
                    ShaclRulesWriter.print(rule, ruleSet.getPrefixMap());
                });
            }
        }
        int m = maxStratum;
//        if ( ! seenDataLayer )
//            m = m - (minDependentStratum-dataStratum);
        return new Stratification(dataStratum, m, layers, ruleSet);
    }
}
