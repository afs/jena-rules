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

package org.seaborne.jena.shacl_rules;

import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.util.Context;

public class Rules {

    public static Context getContext() {
        return ARQ.getContext();
    }

    /**
     * Rule equivalence is defined as two rules being the same for execution.
     * but they may be different by object identity and serialization (speciifcally, order in the head and body).
     * In java terms, {@code rule1 != rule2}.
     */
    public static boolean sameAs(Rule rule1, Rule rule2) {
        return rule1.equivalent(rule2);
    }

    /**
     * Test whether two ruleset are equivalent for the purposes of execution.
     * This is weaker that ".equals" which means same order of tuples in the head,
     * same order of tuples, filters, and assignments in the body.
     */
    public static boolean equivalentRuleSets(RuleSet ruleSet1, RuleSet ruleSet2) {
        return RuleSet.equivalentRuleSets(ruleSet1, ruleSet2);
    }
}
