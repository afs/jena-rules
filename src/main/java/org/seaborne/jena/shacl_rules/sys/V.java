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

package org.seaborne.jena.shacl_rules.sys;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.jena.shacl_rules.expr.NX;
import org.seaborne.jena.shacl_rules.rdf_syntax.RVar;

public
class V {
    public static Node NIL = RDF.Nodes.nil;
    public static Node CAR = RDF.Nodes.first;
    public static Node CDR = RDF.Nodes.rest;
    public static Node TYPE = RDF.Nodes.type;

    static final String SH = "http://www.w3.org/ns/shacl#";
    static final String SHR = "http://www.w3.org/ns/shacl-rules#";
    static final String SHNEX = NX.SHNEX;

    private static Node uri(String localName) { return uri(SHR, localName); }
    private static Node uri(String namespace, String localName) { return NodeFactory.createURI(namespace+localName); }

    // ---- RDF Rules syntax

    public static final Node classRule = uri("Rule");
    public static final Node head = uri("head");
    public static final Node body = uri("body");

    public static final Node rule = uri("rule");

    public static final Node classRuleSet = uri("RuleSet");
    //public static final Node ruleSet = uri("ruleSet");
    // Property connecting to the rule sequence of a rule set.
    public static final Node rules = uri("rules");
    public static final Node data = uri("data");

    public static final Node subject = uri("subject");
    public static final Node predicate = uri("predicate");
    public static final Node object = uri("object");

    public static final Node negation = uri("not");
    public static final Node assign = uri("assign");
    public static final Node assignVar = uri("assignVar");
    public static final Node assignValue = uri("assignValue");

    /**
     * The property that gives a resource (usually a blank node) a variable name.
     * In application code, prefer using {@link RVar#getVar}/{@link RVar#addVar}/{@link RVar#getVarName}
     * over accessing the graph using V.var.
     */

    // Node expressions
    // Synonym for shnex:var
//    public static final Node varName = uri(SHNEX, "var");
//    public static final Node varName = NX.var;
    public static final Node varName = uri("varName");

    public static final Node ifCond = uri("if");
    public static final Node ifThen = uri("then");
    public static final Node ifElse = uri("else");

    // Bad name? Use type instead?
    public static final Node sparqlExpr = uri("sparqlExpr");
    public static final Node expr = uri("expr");

    // Temp?
    public static final Node sparqlBody = uri("sparqlBody");

//    /** Class for list argument node expressions */
//    public static final Node exprClass = uri("Expression");

    /** Class for node expressions that are SPARQL expressions. */
    public static final Node classSparqlExpr = uri("SPARQLExpr");
}