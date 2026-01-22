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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiMapUtils;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.ShaclRulesWriter;
import org.seaborne.jena.shacl_rules.sys.DependencyGraph.Edge;

/**
 * A stratification of a rule set.
 */
public class Stratification {
    // Later - combine with "Connected components" and SCCs
    // See also RcursionChecker

    final private int minStratum;
    final private int maxStratum;
    final private ListValuedMap<Integer, Rule> stratumLevels;

    // Setting to one separates data rules (no dependencies) from rule with rule dependencies.
    static Integer dataStratum = Integer.valueOf(0);
    // Setting to one separates data rules (no dependencies) from rule with rule dependencies.
    static Integer minDependentStratum = Integer.valueOf(1);

    public static Stratification create(RuleSet ruleSet, DependencyGraph depGraph) {
        return createStratification(dataStratum, depGraph.getRuleSet(), depGraph);
    }

    private Stratification(int minStratum, int maxStratum,  ListValuedMap<Integer, Rule> stratumLevels) {
        this.minStratum = minStratum;
        this.maxStratum = maxStratum;
        this.stratumLevels = stratumLevels;
    }

    public List<Rule> getLevel(int i) {
        return stratumLevels.get(i);
    }

    public List<Rule> getLevel(Integer i) {
        return stratumLevels.get(i);
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
    public void forEach(BiConsumer<Integer, Rule> action) {
        MapIterator<Integer, Rule> iter = stratumLevels.mapIterator();
        while(iter.hasNext()) {
            action.accept(iter.next(), iter.getValue());
        }
    }

    // ----
    private static Stratification createStratification(Integer dataStratum, RuleSet ruleSet, DependencyGraph depGraph) {
        // The results.
        Map<Rule, Integer> stratumMap = new HashMap<>();
        ListValuedMap<Integer, Rule> stratumLevels = MultiMapUtils.newListValuedHashMap();
        int maxStratum = 0;

        // ---- StratumMap

        // Initialize all rules to stratum zero.
        ruleSet.getRules().forEach(rule-> {
            if ( depGraph.isDataRule(rule) )
                stratumMap.put(rule, dataStratum);
            else
                stratumMap.put(rule, minDependentStratum);
        });

        // Bad recursion would cause this to go on forever.
        boolean changed = true;

        // Upper bound on the number of strata.
        // Only the dataStraum can have zero rules.
        // Otherwise, each stratum has at least one rule and
        // level numbering has no gaps.
        // So if every stratum is filled with one rule, there can be less that this many strata:
        // (+1 is for the data stratum)
        final int limit = ruleSet.getRules().size()+1;

        while(changed) {
            changed = false;
            for ( Edge e : depGraph.edges() ) {

                // Edge from p to q of type sign
                Rule pRule = e.rule();
                Rule qRule = e.linkedRule();

                switch(e.link()) {
                    case POSITIVE -> {
                        if ( stratumMap.get(pRule) < stratumMap.get(qRule) ) {
                            stratumMap.put(pRule, stratumMap.get(qRule));
                            changed = true;
                        }
                    }
                    case NEGATIVE -> {
                        if ( stratumMap.get(pRule) <= stratumMap.get(qRule) ) {
                            int xStratum = 1 + stratumMap.get(qRule);
                            if ( xStratum > limit )
                                throw new RulesException("Stratification error");
                            stratumMap.put(pRule, xStratum);
                            changed = true;
                        }
                    }
                    //case AGGREGATE->{}
                    default -> {}
                }
            }
        }

        // ---- Levels : reindex as level number -> list of rules.
        // Record the maximum stratum seen.

        for ( Entry<Rule, Integer> entry : stratumMap.entrySet() ) {
            Rule rule = entry.getKey();
            Integer stratumNum = entry.getValue();
            maxStratum = Math.max(maxStratum, stratumNum);
            stratumLevels.put(stratumNum, rule);
        }

        if ( false ) {
            // Development.
            System.out.println();
            System.out.println("==== Strata");
            System.out.printf("====   max = %d\n", maxStratum);
            for ( int i = 0 ; i < maxStratum ; i++ ) {
                List<Rule> rules = stratumLevels.get(i);
                if ( rules == null || rules.isEmpty() ) {
                    System.err.printf("No rules at level %d\n", i);
                    continue;
                }
                System.out.printf("Level %d\n", i);
                rules.forEach(rule-> {
                    System.out.print("  ");
                    ShaclRulesWriter.print(rule, ruleSet.getPrefixMap());
                });
            }
        }
        return new Stratification(dataStratum, maxStratum, stratumLevels);
    }
}