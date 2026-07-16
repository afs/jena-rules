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

package org.seaborne.jena.shacl_rules.lang;

import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;

public class RuleVisitorWalk {

    /*
     * API
     */

    public static void walk(RuleSet ruleSet, RuleVisitor ruleVisitor) {
        walk0(ruleSet, ruleVisitor);
    }

    private static void walk0(RuleSet ruleSet, RuleVisitor ruleVisitor) {
        //ruleSet.getDataTriples().forEach(_triple->{});

        ruleSet.getRules().forEach(rule->{
            walk(rule, ruleVisitor);
        });

        //ruleSet.getDataTuples().forEach(_tuple->{});
    }

    private static void walk(Rule rule, RuleVisitor ruleVisitor) {
        rule.getHeadElements().forEach(headElt->{
            walk(headElt, ruleVisitor);
        });
        rule.getBodyElements().forEach(bodyElt->{
            walk(bodyElt, ruleVisitor);
        });
    }

    private static void walk(RuleHeadElement headElt, RuleVisitor ruleVisitor) {
        headElt.visit(ruleVisitor);
    }

    private static void walk(RuleBodyElement bodyElt, RuleVisitor ruleVisitor) {
        bodyElt.visit(ruleVisitor);
    }
}
