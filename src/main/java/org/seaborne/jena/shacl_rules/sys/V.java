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

package org.seaborne.jena.shacl_rules.sys;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.jena.shacl_rules.nexpr.NX;

/**
 * Vocabulary relating to SHALC rules, including importing terms from elsewhere.
 * Expected use is as a static import.
 */
public class V {
    public static Node NIL = RDF.Nodes.nil;
    public static Node CAR = RDF.Nodes.first;
    public static Node CDR = RDF.Nodes.rest;
    public static Node TYPE = RDF.Nodes.type;

    private static Node uri(String localName) { return uri(P.SRL, localName); }
    private static Node uri(String namespace, String localName) { return NodeFactory.createURI(namespace+localName); }

    // ---- RDF Rules syntax

    //@formatter:off
    public static final Node classRuleSet   = uri("RuleSet");
    //public static final Node ruleSet = uri("ruleSet");
    // Property connecting to the rule sequence of a rule set.
    public static final Node rules          = uri("rules");

    public static final Node classRule      = uri("Rule");
    public static final Node head           = uri("head");
    public static final Node body           = uri("body");
    public static final Node data           = uri("data");

    // Might be used if rules are linked to a rule set and not in a list.
    //public static final Node rule           = uri("rule");

    // Block of tuples
    public static final Node dataTuples     = uri("tuples");
    // Tuple in head/body
    public static final Node tuple          = uri("tuple");

    public static final Node subject        = uri("subject");
    public static final Node predicate      = uri("predicate");
    public static final Node object         = uri("object");

    public static final Node filter         = uri("filter");
    public static final Node negation       = uri("not");
    public static final Node assign         = uri("assign");
    public static final Node assignVar      = uri("assignVar");
    public static final Node assignValue    = uri("assignValue");

    public static final Node varName        = uri("varName");
    public static final Node varNameAlt     = NX.var;
    // Optional property for node expressions
    public static final Node expr           = uri("expr");

    // These are the SPARQL IF-THEN-ELSE special form, not shnex named parameter node expression.
    public static final Node ifCond         = uri("if");
    public static final Node ifThen         = uri("then");
    public static final Node ifElse         = uri("else");
    //@formatter:on
}
