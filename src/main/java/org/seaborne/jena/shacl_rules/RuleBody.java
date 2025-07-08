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

package org.seaborne.jena.shacl_rules;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.lang.RuleElement;

public class RuleBody {

    private final List<RuleElement> body;
    private final List<Triple> bodyTriples;

    public RuleBody(List<RuleElement> ruleElts) {
        this.body = ruleElts;
        this.bodyTriples = ruleGetTriples(ruleElts);
    }

    // The triples used in pattern matching.
    private static List<Triple> ruleGetTriples(List<RuleElement> ruleElts) {
        List<Triple> x = ruleElts.stream()
                .map(RuleBody::patternTripleOrNull)
                .filter(Objects::nonNull).toList();
        return x;
    }

    private static Triple patternTripleOrNull(RuleElement elt) {
        return switch(elt) {
            case RuleElement.EltTriplePattern el -> el.triplePattern();
            default -> null;
        };
    }

    public List<RuleElement> getBodyElements() {
        return body;
    }

    public void forEach(Consumer<RuleElement> action) {
        body.forEach(action);
    }

    /**
     * The triples used in pattern matching.
     */
    public List<Triple> getDependentTriples() {
        return bodyTriples;
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

    @Override
    public String toString() {
        List<Triple> x = body.stream()
                .map(elt-> switch(elt) {
                    case RuleElement.EltTriplePattern el -> el.triplePattern();
                    default -> null;
                })
                .filter(Objects::nonNull).toList();
        return x.toString();
    }
}
