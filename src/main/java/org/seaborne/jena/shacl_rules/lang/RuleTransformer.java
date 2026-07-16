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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.tuples.Tuple;

public class RuleTransformer {

    /*
     *
     */

    public static void transform(RuleSet ruleSet, RuleTransform transform) {
        transform0(ruleSet, transform);
    }

    private static void transform0(RuleSet ruleSet, RuleTransform transform) {
        ruleSet.getData();          // Graph
        ruleSet.getDataTriples();

        List<Rule> newRules = new ArrayList<>(ruleSet.getRules().size());

        ruleSet.getRules().forEach(rule->{
            Rule r = transform(rule, transform);
        });

        ruleSet.getDataTuples();
        ruleSet.getTupleStore();    // TupleStore
    }




    private static Rule transform(Rule rule, RuleTransform transform) {
//        List<RuleHeadElement> newHeadElts = new ArrayList<>(rule.getHeadElements().size());
//        rule.getHeadElements().forEach(headElt->{
//            RuleHeadElement elt = transform(headElt, transform);
//            newHeadElts.add(elt);
//        });
//        List<RuleBodyElement> newBodyElts = new ArrayList<>(rule.getBodyElements().size());
//        rule.getBodyElements().forEach(bodyElt->{
//            RuleBodyElement elt = transform(bodyElt, transform);
//            newBodyElts.add(elt);
//        });

//        List<RuleHeadElement> newHeadElts =
//                rule.getHeadElements().stream()
//                    .map(elt->transform(elt, transform))
//                    .toList();
//        List<RuleBodyElement> newBodyElts =
//                rule.getBodyElements().stream()
//                    .map(elt->transform(elt, transform))
//                    .toList();

        Rule.Builder builder = Rule.newBuilder();
        rule.getHeadElements().forEach(elt->{
            RuleHeadElement newElt = transform(elt, transform);
            builder.addHeadElement(newElt);
        });
        rule.getBodyElements().forEach(elt->{
            RuleBodyElement newElt = transform(elt, transform);
            builder.addBodyElement(newElt);
        });
        builder.ruleIdentifier(rule.getId());
        builder.groundedRule(rule.isGrounded()); // ???
        return builder.build();
    }

    private static RuleHeadElement transform(RuleHeadElement headElt, RuleTransform transform) {
        return switch(headElt) {
            case RuleHeadElement.EltTripleTemplate x -> {
                Triple newTriple = x.tripleTemplate();
                yield transform.transform(x, newTriple);
            }
            case RuleHeadElement.EltTupleTemplate x -> {
                Tuple newTuple = x.tupleTemplate();
                yield transform.transform(x, newTuple);
            }
            case null -> { throw new NullPointerException(); }
            default -> { throw new InternalErrorException(); }
        };
    }

    private static RuleBodyElement transform(RuleBodyElement bodyElt, RuleTransform transform) {
        switch(bodyElt) {
            case RuleBodyElement.EltTriplePattern x -> {
                Triple newTriple = x.triplePattern();
                return transform.transform(x, newTriple);
            }
            case RuleBodyElement.EltTuplePattern x -> {
                Tuple newTuple = x.tuplePattern();
                return transform.transform(x, newTuple);
            }
            case RuleBodyElement.EltNegation x -> {
                // XXX replace grounded?
                List<RuleBodyElement> newInner = transform(x.inner(), transform);
                return transform.transform(x, newInner, x.grounded());
            }
            case RuleBodyElement.EltFilter x -> {
                return transform.transform(x, x.condition());
            }
            case RuleBodyElement.EltAssignment x -> {
                return transform.transform(x, x.var(), x.expression());
            }
            case null -> { throw new NullPointerException(); }
            default -> { throw new InternalErrorException(); }
        }
    }

    // XXX Decide consistent style!
    private static List<RuleBodyElement> transform(List<RuleBodyElement> bodyElts, RuleTransform transform) {
        return bodyElts.stream()
                        .map(elt->transform(elt, transform))
                        .toList();
    }
}
