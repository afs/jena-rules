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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIs;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.rdf_syntax.GraphToRuleSet;
import org.seaborne.jena.shacl_rules.sys.P;
import org.seaborne.jena.shacl_rules.tuples.Tuple;

public class Rule {

    private final RuleHead head;
    private final RuleBody body;

    // RDF Term that identifies this rule.
    // Usually, a URI.
    // Maybe null; the global identifier for external reference to this rule.

    public final Node ruleIdentifier;

    private final boolean isGrounded;
    private final boolean hasAssignment;
    private final boolean hasNegation;
    //boolean final hasAggregation;
    private final boolean hasTemplateBNodes;

    /**
     * Used by the parser and {@link GraphToRuleSet}
     */
    public static Rule create(List<RuleHeadElement> headElts, List<RuleBodyElement> bodyElts) {
        return create(null, headElts, bodyElts);
    }

    /** Parse a string, expecting prefixes, exactly one rule, and nothing else.  */
    public static Rule parseRule(String str) {
        RuleSet ruleset = ShaclRules.parseString(str);
        if ( ruleset.hasData() || ruleset.hasImports() || ruleset.hasTupleData() )
            throw new ShaclRulesParseException("String has other items", -1, -1);
        if ( ruleset.getRules().size() != 1 )
            throw new ShaclRulesParseException("String does not have exactly one rule", -1, -1);
        return ruleset.getRules().getFirst();
    }


    /**
     * Used by the parser and {@link GraphToRuleSet}
     */
    public static Rule create(String iriStr, List<RuleHeadElement> headElts, List<RuleBodyElement> bodyElts) {
        return Rule.newBuilder()
                .ruleIdentifier(iriStr)
                .addHeadElements(headElts)
                .addBodyElements(bodyElts)
                .build();
    }

    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        private List<RuleHeadElement> headElts = new ArrayList<>();
        private List<RuleBodyElement> bodyElts = new ArrayList<>();

        // RDF Term that identifies this rule.
        // Usually, a URI.

        private Node ruleIdentifier;

        // Convenience if within the rule set.
        private String localId;

        private boolean hasAssignment = false;
        private boolean hasNegation = false;
        //boolean final hasAggregation;
        private boolean hasTemplateBNodes = false;
        private boolean groundedRule = false;

        public Builder addHeadElement(RuleHeadElement elt)  {
            if ( elt != null )
                headElts.add(elt);
            return this;
        }

        public Builder addBodyElement(RuleBodyElement elt)  {
            if ( elt != null )
                bodyElts.add(elt);
            return this;
        }

        public Builder addHeadElements(Collection<RuleHeadElement> elts)  {
            headElts.addAll(elts);
            return this;
        }

        public Builder addBodyElements(Collection<RuleBodyElement> elts)  {
            bodyElts.addAll(elts);
            return this;
        }

        public Builder ruleIdentifier(String iriStr) {
            if ( iriStr != null ) {
                IRIs.check(iriStr);
                this.ruleIdentifier = NodeFactory.createURI(iriStr);
            } else {
                this.ruleIdentifier = null;
            }
            return this;
        }

        public Builder ruleIdentifier(Node ruleIdentifier) {
            this.ruleIdentifier = ruleIdentifier;
            return this;
        }

        public Builder groundedRule(boolean groundedRule) {
            this.groundedRule = groundedRule;
            return this;
        }

        private boolean blankNodePresent(Triple triple) {
            if ( blankNodePresent(triple.getSubject()) )
                return true;
            //if ( containsBNode(triple.getPredicate()) ) {}
            if ( blankNodePresent(triple.getObject()) )
                return true;
            return false;
        }

        private boolean blankNodePresent(Node node) {
            if ( node.isTripleTerm() ) {
                return blankNodePresent(node.getTriple());
            }
            return node.isBlank();
        }

        public Rule build() {
            // Rules don't always come from the SRL parser.
            boolean _hasAssignment = false;
            boolean _hasNegation = false;
            //boolean final hasAggregation;

            // Look for assignment and negation
            for ( RuleBodyElement elt : bodyElts ) {
                switch (elt) {
                    // case RuleBodyElement.EltTriplePattern(Triple triplePattern) -> {}
                    // case RuleBodyElement.EltTuplePattern(Tuple tuplePattern) -> {}
                    case RuleBodyElement.EltNegation(List<RuleBodyElement> inner, boolean grounded) -> { _hasNegation = true; }
                    // case RuleBodyElement.EltFilter(Expr condition) -> {}
                    case EltAssignment(Var var, Expr expression) -> { _hasAssignment = true; }
                    case null -> {}
                    default -> {}
                };
            }

            // Look for blank nodes in the head template
            boolean _hasHeadBNodes = false;
            for ( RuleHeadElement elt : headElts ) {
                switch (elt) {
                    case RuleHeadElement.EltTripleTemplate(Triple tripleTemplate) -> {
                        if ( blankNodePresent(tripleTemplate ) )
                            _hasHeadBNodes = true;
                    }
                    //case RuleHeadElement.EltTupleTemplate(Tuple tupleTemplate) -> {}
                    case null -> {}
                    default -> {}
                }
            }

            return new Rule(ruleIdentifier, headElts, bodyElts,
                            groundedRule,
                            _hasAssignment, _hasNegation, _hasHeadBNodes);
        }

    }

    private Rule(Node ruleIdenifier, List<RuleHeadElement> headElts, List<RuleBodyElement> bodyElts,
                 boolean isGrounded,
                 boolean hasAssignment, boolean hasNegation, boolean hasTemplateBNodes
                 //, boolean hasAggregation
                 ) {
        this.head = new RuleHead(headElts);
        this.body = new RuleBody(bodyElts);
        this.ruleIdentifier = ruleIdenifier;

        this.isGrounded = isGrounded;
        this.hasAssignment = hasAssignment;
        this.hasNegation = hasNegation;
        //this.hasAggregation = hasAggregation;
        this.hasTemplateBNodes = hasTemplateBNodes;
    }

    public boolean isRunOnceRule() {
        return hasAssignment || hasTemplateBNodes || isGrounded;
    }

    public boolean hasAssignment() {
        return hasAssignment;
    }

    public boolean hasNegation() {
        return hasNegation;
    }

    // boolean final hasAggregation;
    public boolean hasTemplateBNodes() {
        return hasTemplateBNodes;
    }

    public boolean hasTemplateBlankNodes() {
        return hasTemplateBNodes;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public Node getId() {
        return ruleIdentifier;
    }

    // -- Head

    public RuleHead getHead() {
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

    public RuleBody getBody() {
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
     * This operation ignores the rule's IRI.
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
        return toString(P.prefixMap());
    }

    public String toString(PrefixMap prefixMap) {
        String x = ShaclRulesWriter.asString(this, prefixMap);
        return x.trim();

//        String x = body.toString();
//        x = x.replace("\n", " ");
//        x = x.replaceAll("  +", " ");
//        return head.toString() + " :- " + x;
    }

}
