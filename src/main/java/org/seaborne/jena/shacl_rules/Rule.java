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

import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.rdf_syntax.GraphToRuleSet;

public class Rule {

    private final RuleHead head;
    private final RuleBody body;

    /**
     * Used by the parser and {@link GraphToRuleSet}
     */
    public static Rule create(List<Triple> triples, List<RuleElement> body) {
        return new Rule(triples, body);
    }

    private Rule(List<Triple> triples, List<RuleElement> body) {
        this.head = new RuleHead(triples);
        this.body = new RuleBody(body);
    }

    public RuleHead getHead() {
        return head;
    }

    // Currently, the parser structure.
    public RuleBody getBody() {
        return body;
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
        return head.toString() + " :- " + x;
    }
}
