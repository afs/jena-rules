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
import java.util.function.Consumer;

import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.rdf_syntax.GraphToRuleSet;
import org.seaborne.jena.shacl_rules.tuples.Tuple;

public class Rule {

    private final RuleHead head;
    private final RuleBody body;
    // Debug!
    private static int counter = 0;
    public final String id;

    /**
     * Used by the parser and {@link GraphToRuleSet}
     */
    public static Rule create(List<RuleHeadElement> headElts, List<RuleBodyElement> bodyElts) {
        return new Rule(headElts, bodyElts);
    }

    private Rule(List<RuleHeadElement> headElts, List<RuleBodyElement> bodyElts) {
        this.head = new RuleHead(headElts);
        this.body = new RuleBody(bodyElts);
        counter++;
        id = ""+counter;

        boolean hasAssignment;
        boolean hasNegation;
        boolean hasAggregation;
        boolean hasHeadBNodes;
    }

    // -- Head

    private RuleHead getHead() {
        return head;
    }

    // XXX This should go away!
    public List<Triple> getHeadTriples() {
        return getHead().getHeadTriples();
    }

    // XXX This should go away!
    public List<Tuple> getHeadTuples() {
        return getHead().getHeadTuples();
    }


    /**
     * Return the triple templates that used to generate triples.
     * The triples in the list may contain named variables.
     */
    public List<RuleHeadElement> getHeadElements() {
        return getHead().getHeadElements();
    }

    public void forEachHeadElement(Consumer<RuleHeadElement> action) {
        getHead().getHeadElements().forEach(action);
    }

    // -- Body

    // Currently, the parser structure.
    private RuleBody getBody() {
        return body;
    }

    public List<RuleBodyElement> getBodyElements() {
        return getBody().getBodyElements();
    }

    public void forEachBodyElement(Consumer<RuleBodyElement> action) {
        getBody().getBodyElements().forEach(action);
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
