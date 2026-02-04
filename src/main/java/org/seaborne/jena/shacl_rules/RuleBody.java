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

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.EltTriplePattern;

class RuleBody {

    private final List<RuleBodyElement> body;

    public RuleBody(List<RuleBodyElement> ruleElts) {
        this.body = ruleElts;
    }

    // The triples used in pattern matching.
    private static List<Triple> ruleGetTriples(List<RuleBodyElement> ruleElts) {
        List<Triple> x = ruleElts.stream()
                .map(RuleBody::patternTripleOrNull)
                .filter(Objects::nonNull)
                .toList();
        return x;
    }

    private static Triple patternTripleOrNull(RuleBodyElement elt) {
        return switch(elt) {
            case EltTriplePattern el -> el.triplePattern();
            default -> null;
        };
    }

    List<RuleBodyElement> getBodyElements() {
        return body;
    }

    public void forEach(Consumer<RuleBodyElement> action) {
        body.forEach(action);
    }

    /**
     * Apply an action to each index and rule, in the order they appear in the body.
     */
    public void forEach(BiConsumer<Integer, RuleBodyElement> action) {
        int N = body.size();
        for ( int i = 0 ; i < N ; i++ ) {
            action.accept(i, body.get(i));
        }
    }

    /**
     * Equivalent - same effect, not necessarily {@code .equals}.
     */
    public boolean equivalent(RuleBody other) {
        // Order does matter evaluation - well-formedness
        return Objects.equals(body, other.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( !(obj instanceof RuleBody) )
            return false;
        RuleBody other = (RuleBody)obj;
        return Objects.equals(body, other.body);
    }

//    @Override
//    public String toString() {
//        // XXX Unfinished
//        body.stream()
//                .map(elt-> switch(elt) {
//                    case EltTriplePattern el -> el.triplePattern();
//                    case EltNegation neg -> { }
//                })
//                .filter(Objects::nonNull).toList();
//        return x.toString();
//    }
}
