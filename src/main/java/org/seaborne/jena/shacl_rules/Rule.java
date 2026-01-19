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
import java.util.function.Consumer;

import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.rdf_syntax.GraphToRuleSet;

public class Rule {

    private final RuleHead head;
    private final RuleBody body;
    // Debug!
    private static int counter = 0;
    public final String id;

    /**
     * Used by the parser and {@link GraphToRuleSet}
     */
    public static Rule create(List<Triple> triples, List<RuleElement> body) {
        return new Rule(triples, body);
    }

    private Rule(List<Triple> triples, List<RuleElement> body) {
        this.head = new RuleHead(triples);
        this.body = new RuleBody(body);
        counter++;
        id = ""+counter;
    }

    // -- Head

    private RuleHead getHead() {
        return head;
    }

    /**
     * Return the triple templates that used to generate triples.
     * The triples in the list may contain named variables.
     */
    public List<Triple> getTripleTemplates() {
        return getHead().getTripleTemplates();
    }

    // -- Body

    // Currently, the parser structure.
    private RuleBody getBody() {
        return body;
    }

    public List<RuleElement> getBodyElements() {
        return getBody().getBodyElements();
    }

    public void forEachBodyElement(Consumer<RuleElement> action) {
        getBody().getBodyElements().forEach(action);
    }

    /**
     * Return the triple patterns that occur in the body, and may depend on other
     * rules, as well as appearing in the abox (the facts of the base graph).
     * The triples in the list may contain named variables.
     */
    @Deprecated(forRemoval = true)
    public List<Triple> getDependentTriples() {
        return getBody().getDependentTriples();
    }

    /**
     * Rule equivalence is defined as two rules being the same for execution.
     * but they may be different by object identity. In java terms, {@code rule1 != rule2}.
     * That is, parser round trip.
     * <p>
     * This means they have the "equivalent head" and "equivalent body", not necessarily the same order of elements.
     * <p>
     * Use via public {@link Rules#sameAs}.
     */
    /*package*/ boolean equivalent(Rule other) {
        if ( ! head.equivalent(other.head) )
            return false;
        if ( ! body.equivalent(other.body) )
            return false;
        return true;
    }

    /**
     * Rule equivalence for serialization (and hence execution).
     * That is, parser round trip.
     * This means they have the "same head" and "same body", and also having the same order of elements.
     */
    /*package*/ boolean equivalentSerialization(Rule other) {
        if ( ! head.equals(other.head) )
            return false;
        if ( ! body.equals(other.body) )
            return false;
        return true;
    }

    @Override
    public String toString() {
        String x = body.toString();
        x = x.replace("\n", " ");
        x = x.replaceAll("  +", " ");
        return "["+id+"]" +head.toString() + " :- " + x;
    }
}
