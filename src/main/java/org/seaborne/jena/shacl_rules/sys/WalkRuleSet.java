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

import java.util.Objects;

import org.seaborne.jena.shacl_rules.RuleSet;

/**
 * Traverse a rule set.
 * <p>
 * This can be useful for separating inspection code from the Rule/RuleSet classes.
 */
public class WalkRuleSet {

    public static void walk(RuleSet ruleSet, RuleSetVisitor ruleSetVisitor) {
        walk(ruleSet, ruleSetVisitor, null);
    }

    public static void walk(RuleSet ruleSet, RuleSetVisitor ruleSetVisitor, RuleVisitor ruleVisitor) {
        Objects.requireNonNull(ruleSet);

        if ( ruleSetVisitor != null ) {
            ruleSetVisitor.startVisitRuleSet();
            ruleSetVisitor.visitPrologue(ruleSet);
            ruleSetVisitor.visitData(ruleSet);
            ruleSetVisitor.visitData(ruleSet);
            ruleSetVisitor.visitRules(ruleSet);
        }

        if ( ruleVisitor != null ) {
            ruleSet.getRules().forEach(rule->{
                ruleVisitor.startVisitRule(rule);
                ruleVisitor.visitRule(rule);
                ruleVisitor.visitHead(rule);
                ruleVisitor.visitBody(rule);
            });
        }
    }
}
