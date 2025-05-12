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

package org.seaborne.jena.shacl_rules.rdf_syntax;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;

class V {
    static Node NIL = RDF.Nodes.nil;
    static Node CAR = RDF.Nodes.first;
    static Node CDR = RDF.Nodes.rest;
    static Node TYPE = RDF.Nodes.type;

    static final String SH = "http://www.w3.org/ns/shacl#";
    private static Node uri(String localName) { return NodeFactory.createURI(SH+localName); }

    static final Node subject = uri("subject");
    static final Node predicate = uri("predicate");
    static final Node object = uri("object");

    static final Node ruleClass = uri("Rule");
    static final Node head = uri("head");
    static final Node body = uri("body");

    static final Node rule = uri("rule");
    static final Node ruleSet = uri("ruleSet");

    static final Node var = uri("var");

    static final Node sparqlExpr = uri("sparqlExpr");

    // Temp
    static final Node sparqlBody = uri("sparqlBody");
}