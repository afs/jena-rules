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
import java.util.ListIterator;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.ExprUtils;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.expr.SparqlNodeExpressions;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltNegation;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltTriplePattern;
import org.seaborne.jena.shacl_rules.sys.RuleLib;
import org.seaborne.jena.shacl_rules.sys.V;

public class RuleSetToGraph {

    public static Graph asGraph(RuleSet ruleSet) {
        Graph graph = GraphFactory.createDefaultGraph();
        if ( ruleSet.hasPrefixMap() )
            graph.getPrefixMapping().setNsPrefixes(Prefixes.adapt(ruleSet.getPrefixMap()));
        writeToGraph(graph, ruleSet);
        return graph;
    }

    public static void writeToGraph(Graph graph, RuleSet ruleSet) {
        Node ruleSetNode = NodeFactory.createURI("http://example/ruleSet-1");
        graph.add(ruleSetNode, V.TYPE, V.classRuleSet);

        // For each rule, write, add the blank node to a list.
        List<Node> rules = new ArrayList<>();
        ruleSet.getRules().forEach(rule->{
            Node ruleNode = NodeFactory.createBlankNode();
            if ( true ) {
                graph.add(ruleNode, V.TYPE, V.classRule);
            } else {
                Node x = NodeFactory.createBlankNode();
                graph.add(ruleNode, V.rule, x);
                ruleNode = x;
            }

            Node nHead = writeHead(graph, ruleNode, rule);
            graph.add(ruleNode, V.head, nHead);

            Node nBody = writeBody(graph, ruleNode, rule);
            graph.add(ruleNode, V.body, nBody);

            rules.add(ruleNode);
        });
        Node rulesList = list(graph, rules);
        graph.add(ruleSetNode, V.rules, rulesList);

        writeData(graph, ruleSet, ruleSetNode);
    }

    // Write data ("Axiomatic triple")
    private static void writeData(Graph graph, RuleSet ruleSet, Node ruleSetNode) {
        if ( ! ruleSet.hasData() )
                return;
        List<Triple> triples = ruleSet.getDataTriples();
        List<Node> tripleTerms = Iter.iter(triples).map(triple->NodeFactory.createTripleTerm(triple)).toList();
        Node x = JLib.addList(graph, tripleTerms);
        graph.add(ruleSetNode, V.data, x);
    }

    private static Node writeHead(Graph graph, Node ruleNode, Rule rule) {
        List<Triple> head = rule.getTripleTemplates();
        List<Node> x = triplesAsList(graph, head);
        Node bgpNode = list(graph, x);
        return bgpNode;
    }


    private static Node writeBody(Graph graph, Node ruleNode, Rule rule) {
        List<RuleElement> bodyElts = rule.getBodyElements();
        Node bodyNode = writeBodyElements(graph, bodyElts);

        if ( false ) {
            // Put in the SPARQL form.
            String qs = RuleLib.ruleEltsToElementGroup(bodyElts).toString();
            Node nSparqlForm = NodeFactory.createBlankNode();
            Node nQueryString = NodeFactory.createLiteralString(qs);
            graph.add(ruleNode, V.sparqlBody, nQueryString);

            // Or add to body:
            //graph.add(nSparqlForm, V.sparqlBody, nQueryString);
            //items.addLast(nSparqlForm);
        }
        return bodyNode;
    }

    private static Node writeBodyElements(Graph graph, List<RuleElement> bodyElts) {
        List<Node> items = new ArrayList<>();
        bodyElts.forEach(elt->{
            switch(elt) {
                case EltTriplePattern(var triplePattern) -> {
                   items.add(encodeTriple(graph, triplePattern));
                }
                case EltCondition(Expr condition) -> {
                    Node nExpr = expression(graph, condition);
                    items.add(nExpr);
                }
                case EltNegation(var innerBody) ->{
                    Node nInnerBody = writeBodyElements(graph, innerBody);
                    Node x1 = NodeFactory.createBlankNode();
                    graph.add(x1, V.negation, nInnerBody);
                    items.add(x1);
                }

                case EltAssignment(Var var, Expr expression) -> {
                    // Functions.
                    Node nExpr = expression(graph, expression);
                    Node nVar = RVar.addVar(graph, var.getVarName());

                    Node x1 = NodeFactory.createBlankNode();
                    graph.add(x1, V.assignVar, nVar);
                    graph.add(x1, V.assignValue, nExpr);

                    Node x2 = NodeFactory.createBlankNode();
                    graph.add(x2, V.assign, x1);
                    items.add(x2);
                }

                case null -> {}
                default -> {}
                }
        });

        Node bodyNode = list(graph, items);
        return bodyNode;
    }

    private static Node expression(Graph graph, Expr expr) {
        Node x = SparqlNodeExpressions.exprToRDF(graph, expr);

        // Direct as sh:sparqlExpr
//      Node x = NodeFactory.createBlankNode();
//      String exprStr = exprAsString(expr);
//      Node obj = NodeFactory.createLiteralString(exprStr);
//      graph.add(x, V.sparqlExpr, obj);
      return x;
    }

    private static String exprAsString(Expr expr) {
        IndentedLineBuffer out = new IndentedLineBuffer();
        ExprUtils.fmtSPARQL(out, expr);
        //WriterSSE.out(out, expr, null);
        return out.asString();
    }

    private static List<Node> triplesAsList(Graph graph, List<Triple> triples) {
        List<Node> elements = new ArrayList<>();
        triples.forEach(triple->{
            var tripleNode = encodeTriple(graph, triple);
            elements.add(tripleNode);
        });
        return elements;
    }

    private static Node encodeTriple(Graph graph, Triple triple) {
        Node tripleNode = NodeFactory.createBlankNode();
        Node sNode = convertTermOrVar(graph, triple.getSubject());
        Node pNode = convertTermOrVar(graph, triple.getPredicate());
        Node oNode = convertTermOrVar(graph, triple.getObject());

        Triple sTriple = Triple.create(tripleNode, V.subject, sNode);
        Triple pTriple = Triple.create(tripleNode, V.predicate, pNode);
        Triple oTriple = Triple.create(tripleNode, V.object, oNode);
        graph.add(sTriple);
        graph.add(pTriple);
        graph.add(oTriple);
        return tripleNode;
    }

    private static Node convertTermOrVar(Graph graph, Node node) {
        // Triple terms
//        if ( node.isTripleTerm() ) {
//                // May have variables.
//        }

        if ( node.isConcrete() )
            // Includes concrete triple terms.
            return node;

        if ( node.isTripleTerm() ) {
            // With variables
            Triple t = node.getTriple();
            Node x = encodeTriple(graph, t);
            return x;
        }

        if ( Var.isVar(node) ) {
            Node varNode = RVar.addVar(graph, node.getName());
            return varNode;
        }
        throw new ShaclException("Node type not recognized:; "+node);
    }

    // Why is this not in GraphList?
    private static Node list(Graph graph, List<Node> elements) {
        ListIterator<Node> iter = elements.listIterator(elements.size());
        Node x = V.NIL;

        while(iter.hasPrevious()) {
            Node cell = NodeFactory.createBlankNode();
            Node elt = iter.previous();
            graph.add(cell, V.CAR, elt);
            graph.add(cell, V.CDR, x);
            x = cell;
        }
        return x;
    }
}
