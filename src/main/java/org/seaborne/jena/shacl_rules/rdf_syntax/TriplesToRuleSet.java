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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.sparql.util.graph.GNode;
import org.apache.jena.sparql.util.graph.GraphList;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;

public class TriplesToRuleSet {

    public static RuleSet parse(Graph graph) {
        List<RuleSet> ruleSets = _parse(graph);
        if ( ruleSets == null )
            return null ;
        if ( ruleSets.isEmpty() )
            return null;
        if ( ruleSets.size() == 1 )
            return ruleSets.getFirst();
        throw new ShaclException("Multiple rule sets in graph");
    }

    public static List<RuleSet> parseAll(Graph graph) {
        List<RuleSet> ruleSets = _parse(graph);
        return ruleSets;
    }

    private static List<RuleSet> _parse(Graph graph) {
        // XXX Need identified data. <<()>>

        List<RuleSet> ruleSets = new ArrayList<>();
        List<Node> ruleSetNodes = Iter.toList(G.iterObjectsOfPredicate(graph, V.ruleSet));

        ruleSetNodes.forEach(ruleSetListNode -> {
            // list of rules

            List<Rule> rules = new ArrayList<>();
            GNode gNode = GNode.create(graph, ruleSetListNode);
            List<Node> ruleNodes = GraphList.members(gNode);
            ruleNodes.forEach(n->{
                Rule r = parseRule(graph, n);
                if ( r != null )
                    rules.add(r);
            });

            RuleSet ruleSet = new RuleSet(null, PrefixMapFactory.create(graph.getPrefixMapping()), rules, null);
            ruleSets.add(ruleSet);
        });

//        // Find by type RuleClass
//        List<Rule> rules = new ArrayList<>();
//        List<Node> ruleNodes = G.listPO(graph, V.TYPE, V.ruleClass);
//        ruleNodes.forEach(n->{
//            Rule r = parseRule(graph, n);
//            if ( r != null )
//                rules.add(r);
//        });

        return ruleSets;
    }

    private static Rule parseRule(Graph graph, Node n) {

        Node headNode = G.getOneSP(graph, n, V.head);
        Node bodyNode = G.getOneSP(graph, n, V.body);

        List<Triple> headTemplate = parseRuleHead(graph, headNode);
        ElementGroup body = parseRuleBody(graph, bodyNode);
        Rule rule = new Rule(headTemplate, body);
        return rule;
    }

    private static List<Triple> parseRuleHead(Graph graph, Node headNode) {
        List<Triple> headTemplate = new ArrayList<>();
        GNode gNode = GNode.create(graph, headNode);
        List<Node> x = GraphList.members(gNode);
        x.forEach(node->{
            Triple t = parseTriple(graph, node);
            headTemplate.add(t);
        });
        return headTemplate;
    }

    private static Triple parseTriple(Graph graph, Node node) {
        Node sn = G.getOneSP(graph, node, V.subject);
        Node pn = G.getOneSP(graph, node, V.predicate);
        Node on = G.getOneSP(graph, node, V.object);
        Node s = extractVar(graph, sn);
        Node p = extractVar(graph, pn);
        Node o = extractVar(graph, on);
        Triple t = Triple.create(s, p, o);
        return t;
    }

    private static ElementGroup parseRuleBody(Graph graph, Node bodyNode) {
        ElementGroup elg = new ElementGroup();
        GNode gNode = GNode.create(graph, bodyNode);
        List<Node> x = GraphList.members(gNode);
        // Mutated
        List<Triple> currentTriples = new ArrayList<>();

        x.forEach(node->{
            // Bnode and S/P/O
            // Bnode and sparqlExpr
            // BNode and sparqlBody
            if ( G.hasProperty(graph, node, V.sparqlExpr) ) {
                Node e = G.getOneSP(graph, node, V.sparqlExpr);
                if ( ! G.isString(e) )
                    throw new ShaclException("Not a simple string: "+e);
                String exprString = G.asString(e);
                Expr expr = ExprUtils.parse(exprString);
                Element elt = new ElementFilter(expr);
                elg.addElement(elt);
            } else if ( G.hasProperty(graph, node, V.subject) ) {
                Triple triple = parseTriple(graph, node);
                elg.addTriplePattern(triple);
            } else if ( G.hasProperty(graph, node, V.sparqlBody) ) {
                // Ignore
            } else {
                // Where?
                throw new ShaclException("Didn't recognized RDF for rule body");
            }
        });

        return elg;
    }

//    private static void flushBGP(ElementGroup elg, List<Triple> triples) {
//        if ( triples == null )
//            return;
//        if ( triples.isEmpty() )
//            return;
//        BasicPattern bgp = new BasicPattern(List.copyOf(triples));
//        Element elt = new ElementTriplesBlock(bgp);
//        elg.addElement(el);
//    }

    private static Node extractVar(Graph graph, Node node) {
        if ( ! node.isBlank() )
            return node;
        Node x = G.getZeroOrOneSP(graph, node, V.var);
        if ( x == null )
            throw new ShaclException("Blank node in pattern or malformed [ sh:var ]");
        return Var.alloc(G.asString(x));
    }

}
