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
import org.apache.jena.sparql.util.graph.GNode;
import org.apache.jena.sparql.util.graph.GraphList;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.rdf_syntax.expr.SparqlNodeExpression;

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
        List<RuleSet> ruleSets = new ArrayList<>();

        // Shape:
        // X rdf:Type sh:RuleSet ;
        //   sh:ruleSet ( ... rules ... )
        //   sh:data ( <<(...)>> ....)

//        // Find by type.
//        List<Node> ruleSetNodes = G.listPO(graph, V.TYPE, V.classRuleSet);
//        ruleSetNodes.forEach(ruleSetNode->{
//            Node listOfRules = G.getOneSP(graph, ruleSetNode, V.ruleSet);
//            RuleSet ruleSet = parseRuleSet(graph, ruleSetNode, listOfRules);
//            ruleSets.add(ruleSet);
//        });

        // Find by property sh:ruleSet.
        List<Triple> ruleSetTriples = G.find(graph, null, V.ruleSet, null).toList();
        ruleSetTriples.forEach(t->{
            Node ruleSetNode = t.getSubject();
            Node listOfRules = t.getObject();
            RuleSet ruleSet = parseRuleSet(graph, ruleSetNode, listOfRules);
            ruleSets.add(ruleSet);
        });

        return ruleSets;
    }

    private static RuleSet parseRuleSet(Graph graph, Node ruleSetNode, Node listOfRules) {
        GNode gNode = GNode.create(graph, listOfRules);
        List<Node> ruleNodes = GraphList.members(gNode);
        List<Rule> rules = new ArrayList<>();
        ruleNodes.forEach(n->{
            Rule r = parseRule(graph, n);
            if ( r != null )
                rules.add(r);
        });
        List<Triple> data = parseData(graph, ruleSetNode);
        RuleSet ruleSet = new RuleSet(null, PrefixMapFactory.create(graph.getPrefixMapping()), rules, data);
        return ruleSet;
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
        for ( Node node : x ) {
            // Two forms:
            if ( G.hasProperty(graph, node, V.expr) || G.hasProperty(graph, node, V.sparqlExpr) ) {
                Expr expr = SparqlNodeExpression.rdfToExpr(graph, node);
                Element elt = new ElementFilter(expr);
                elg.addElement(elt);
                continue;
            }

            if ( G.hasProperty(graph, node, V.subject) ) {
                // Single triple rule.
                Triple triple = parseTriple(graph, node);
                elg.addTriplePattern(triple);
                continue;
            }
            if ( G.hasProperty(graph, node, V.sparqlBody) ) {
                // Ignore
                continue;
            }
            throw new ShaclException("Didn't recognized RDF for rule body");
        }
        return elg;
    }

    private static List<Triple> parseData(Graph graph, Node ruleSetNode) {
        // [] sh:data ( <<( )>> .... )
        // or
        // [] sh:data <<( )>>; sh:data <<( )>>; ... .
        // Or named graph

        if ( ! G.hasProperty(graph, ruleSetNode, V.data) )
            return null;
        Node list = G.getOneSP(graph, ruleSetNode, V.data);

        GNode gnode = GNode.create(graph, list);
        List<Node> tripleTerms = GraphList.members(gnode);
        List<Triple> triples = new ArrayList<>();
        tripleTerms.forEach(tt-> triples.add(tt.getTriple()));
        return triples;
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
