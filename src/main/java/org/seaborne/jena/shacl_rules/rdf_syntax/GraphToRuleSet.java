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

package org.seaborne.jena.shacl_rules.rdf_syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.util.graph.GNode;
import org.apache.jena.sparql.util.graph.GraphList;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement.EltTripleTemplate;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement.EltTupleTemplate;
import org.seaborne.jena.shacl_rules.nexpr.SrlExpressions;
import org.seaborne.jena.shacl_rules.sys.V;
import org.seaborne.jena.shacl_rules.tuples.Tuple;

public class GraphToRuleSet {
    // Shape:
    // X rdf:Type sh:RuleSet ;
    //   sh:ruleSet ( ... rules ... )

    /**
     * Parse a rule set out of an RDF graph.
     * This is a convenient operation - beware if the graph contains more than one ruleset,
     * the rule set returned is a random choice ("first found").
     * Return null if no rule set found.
     */
    public static RuleSet parse(Graph graph) {
        //G.subjectsOf(any, V.rules, any);
        Node ruleset = G.getPO(graph, V.rules, null);
        if ( ruleset == null )
            return null;
        return parse(graph, ruleset);
    }

    /**
     * Parse a rule set out of an RDF graph
     * starting at a given node.
     * Return null for no rule set.
     */
    public static RuleSet parse(Graph graph, Node ruleSet) {
        return parseRuleSet(graph, ruleSet);
    }

    /**
     * Parse all rule sets in a graph.
     */
    public static List<RuleSet> parseAll(Graph graph) {
        List<RuleSet> ruleSets = new ArrayList<>();
        List<Triple> ruleSetTriples = G.find(graph, null, V.rules, null).toList();
        ruleSetTriples.forEach(t->{
            Node ruleSetNode = t.getSubject();
            Node listOfRules = t.getObject();
            RuleSet ruleSet = parseRuleSet(graph, ruleSetNode, listOfRules);
            ruleSets.add(ruleSet);
        });
        return ruleSets;
    }

    private static RuleSet parseRuleSet(Graph graph, Node ruleSetNode) {
        Node theRules = G.getSP(graph, ruleSetNode, V.rules);
        if ( theRules == null )
            return null;
        return parseRuleSet(graph, ruleSetNode, theRules);
    }

    private static RuleSet parseRuleSet(Graph graph, Node ruleSetNode, Node theRules) {
        List<Node> ruleNodes = G.listMembers(graph, theRules);
        List<Rule> rules = new ArrayList<>();

        ruleNodes.forEach(n->{
            Rule r = parseRule(graph, n);
            if ( r != null )
                rules.add(r);
        });

        List<Triple> data = parseData(graph, ruleSetNode);
        List<Tuple> tupleData = parseTupleData(graph, ruleSetNode);
        Set<String> imports = null;
        RuleSet ruleSet = RuleSet.create(null, PrefixMapFactory.create(graph.getPrefixMapping()), imports, rules, data, tupleData);
        return ruleSet;
    }

    private static Rule parseRule(Graph graph, Node n) {
        String iri = null;
        if ( n.isURI() )
            iri = n.getURI();

        Node headNode = G.getOneSP(graph, n, V.head);
        Node bodyNode = G.getOneSP(graph, n, V.body);

        List<RuleHeadElement> headTemplate = parseRuleHead(graph, headNode);
        List<RuleBodyElement> body = parseRuleBody(graph, bodyNode);
        Rule rule = Rule.create(iri, headTemplate, body);
        return rule;
    }

    private static List<RuleHeadElement> parseRuleHead(Graph graph, Node headNode) {
        List<RuleHeadElement> headTemplate = new ArrayList<>();
        GNode gNode = GNode.create(graph, headNode);
        List<Node> x = GraphList.members(gNode);
        for ( Node node : x ) {
            if ( G.hasProperty(graph, node, V.subject) ) {
                // Single triple rule.
                Triple triple = parseTriple(graph, node);
                headTemplate.add(new EltTripleTemplate(triple));
                continue;
            }

            if ( G.hasProperty(graph, node, V.tuple) ) {
                // Single tuple rule.
                Node list = G.getOneSP(graph, node, V.tuple);
                Tuple tuple = parseTuple(graph, list);
                headTemplate.add(new EltTupleTemplate(tuple));
                continue;
            }
        }
        return headTemplate;
    }

    private static Triple parseTriple(Graph graph, Node node) {
        Node sn = G.getOneSP(graph, node, V.subject);
        Node pn = G.getOneSP(graph, node, V.predicate);
        Node on = G.getOneSP(graph, node, V.object);
        Node s = getEncodedTermOrVar(graph, sn);
        Node p = getEncodedTermOrVar(graph, pn);
        Node o = getEncodedTermOrVar(graph, on);
        Triple t = Triple.create(s, p, o);
        return t;
    }

    // Translate into a variable or return the node.
    private static Node getEncodedTermOrVar(Graph graph, Node node) {
        if ( ! node.isBlank() )
            return node;

        // Maybe a triple term.
        boolean looksLikeTripleTerm = G.contains(graph, node, V.subject, null);
        if ( looksLikeTripleTerm ) {
            Node st = G.getOneSP(graph, node, V.subject);
            Node pt = G.getOneSP(graph, node, V.predicate);
            Node ot = G.getOneSP(graph, node, V.object);
            Node s = getEncodedTermOrVar(graph, st);
            Node p = getEncodedTermOrVar(graph, pt);
            Node o = getEncodedTermOrVar(graph, ot);
            return NodeFactory.createTripleTerm(s, p, o);
        }

        Var x = RVar.getVar(graph, node);
        if ( x != null )
            return x ;

        throw new ShaclException("Blank node in pattern or malformed for a variable.");
    }

    private static List<RuleBodyElement> parseRuleBody(Graph graph, Node bodyNode) {
        List<RuleBodyElement> body = new ArrayList<>();
        GNode gNode = GNode.create(graph, bodyNode);
        List<Node> x = GraphList.members(gNode);
        // Mutated
        List<Triple> currentTriples = new ArrayList<>();
        for ( Node node : x ) {
            if ( G.hasProperty(graph, node, V.subject) ) {
                // Single triple rule.
                Triple triple = parseTriple(graph, node);
                body.add(new EltTriplePattern(triple));
                continue;
            }

            if ( G.hasProperty(graph, node, V.tuple) ) {
                // Single tuple rule.
                Tuple tuple = parseTuple(graph, node);
                body.add(new EltTuplePattern(tuple));
                continue;
            }
            if ( G.hasProperty(graph, node, V.filter) ) {
                // XXX [RDF syntax] Deal with both V.expr
                Node exprNode = G.getOneSP(graph, node, V.filter) ;
                Expr expr = SrlExpressions.rdfToExpr(graph, exprNode);
                body.add(new EltCondition(expr));
                continue;
            }

            if ( G.hasProperty(graph, node, V.negation) ) {
                Node nInnerBody = G.getOneSP(graph, node, V.negation);
                List<RuleBodyElement> innerBody = parseRuleBody(graph, nInnerBody);
                body.add(new EltNegation(innerBody));
                continue;
            }

            if ( G.hasProperty(graph, node, V.assign) ) {
                Node assign = G.getOneSP(graph, node, V.assign) ;
                Node varNode= G.getOneSP(graph, assign, V.assignVar);
                Var var = RVar.getVar(graph, varNode);
                Node exprNode = G.getOneSP(graph, assign, V.assignValue);
                // Force expr
                //Expr expr = SrlExpressions.rdfToExpr(graph, exprNode);
                Expr expr = SrlExpressions.rdfToExpr(graph, exprNode);
                body.add(new EltAssignment(var, expr));
                continue;
            }

            throw new ShaclException("Didn't recognized RDF for rule body");
        }
        return body;
    }

    private static List<Triple> parseData(Graph graph, Node ruleSetNode) {
        // [] srl:data ( <<( )>> .... )
        // or
        // [] srl:data <<( )>>; sh:data <<( )>>; ... .
        // Or named graph
        // Or a srl:subject/srl:predicate/srl:object

        if ( ! G.hasProperty(graph, ruleSetNode, V.data) )
            return null;
        Node list = G.getOneSP(graph, ruleSetNode, V.data);

        GNode gnode = GNode.create(graph, list);
        List<Node> tripleTerms = GraphList.members(gnode);  // Truiple terms.
        List<Triple> triples = new ArrayList<>();
        tripleTerms.forEach(tt-> {
            Triple triple = parseDataTriple(graph, tt);
            triples.add(triple);
        });
        return triples;
    }

    private static Triple parseDataTriple(Graph graph, Node node) {
        if ( node.isTripleTerm() )
            return node.getTriple();
        Node s = G.getOneSP(graph, node, V.subject);
        Node p = G.getOneSP(graph, node, V.predicate);
        Node o = G.getOneSP(graph, node, V.object);
        Triple triple = Triple.create(s, p, o);
        return triple;
    }

    // List of lists
    // [] srl:tuples ( ( t1 t2 ) (t3) )
    private static List<Tuple> parseTupleData(Graph graph, Node ruleSetNode) {
        if ( ! G.hasProperty(graph, ruleSetNode, V.dataTuples) )
            return null;
        Node list = G.getOneSP(graph, ruleSetNode, V.dataTuples);
        List<Node> tuples = G.listMembers(graph, list);
        List<Tuple> tupleData = new ArrayList<>();
        tuples.forEach(tupleNode-> {
            Tuple tuple = parseTuple(graph, tupleNode);
            tupleData.add(tuple);
        });
        return tupleData;
    }

    // Call with the head of the tuple elements list.
    private static Tuple parseTuple(Graph graph, Node tupleNode) {
        List<Node> tupleArgTerms = G.listMembers(graph, tupleNode);
        Tuple tuple = Tuple.create(tupleArgTerms);
        return tuple;
    }
}
