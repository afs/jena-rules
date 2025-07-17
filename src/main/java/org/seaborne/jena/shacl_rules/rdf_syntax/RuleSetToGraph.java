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
import org.apache.jena.atlas.lib.NotImplemented;
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
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltTriplePattern;
import org.seaborne.jena.shacl_rules.sys.RuleLib;

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
            Node nBody = writeBody(graph, ruleNode, rule);
            rules.add(ruleNode);
        });
        Node rulesList = list(graph, rules);
        graph.add(ruleSetNode, V.ruleSet, rulesList);

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
        graph.add(ruleNode, V.head, bgpNode);
        return bgpNode;
    }

    private static Node writeBody(Graph graph, Node ruleNode, Rule rule) {
        var bodyElts = rule.getBodyElements();
        List<Node> items = new ArrayList<>();
        bodyElts.forEach(elt->{
            switch(elt) {
                case EltTriplePattern(var triplePattern) -> {
                   items.add(encodeTriple(graph, triplePattern));
                }
                case EltCondition(var condition) -> {
                    Node nExpr = expression(graph, condition);
                    items.add(nExpr);
                }
                case EltAssignment(Var var, Expr expression) -> {
                    throw new NotImplemented();
                }
                case null -> {}
                default -> {}
                }
        });

        if ( false ) {
            // Put in the SPARQL form.
            String qs = RuleLib.ruleEltsToElementGroup(bodyElts).toString();
            Node nSparqlForm = NodeFactory.createBlankNode();
            Node nQueryString = NodeFactory.createLiteralString(qs);
            graph.add(nSparqlForm, V.sparqlBody, nQueryString);
            items.addLast(nSparqlForm);
        }
        Node bodyNode = list(graph, items);
        graph.add(ruleNode, V.body, bodyNode);
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
        Node sNode = convertVar(graph, triple.getSubject());
        Node pNode = convertVar(graph, triple.getPredicate());
        Node oNode = convertVar(graph, triple.getObject());

        Triple sTriple = Triple.create(tripleNode, V.subject, sNode);
        Triple pTriple = Triple.create(tripleNode, V.predicate, pNode);
        Triple oTriple = Triple.create(tripleNode, V.object, oNode);
        graph.add(sTriple);
        graph.add(pTriple);
        graph.add(oTriple);
        return tripleNode;
    }

    private static Node convertVar(Graph graph, Node node) {
        // Triple terms
        if ( node.isTripleTerm() )
            throw new ShaclException("Not implementned: triple terms; "+node);
        if ( node.isConcrete() )
            return node;
        if ( Var.isVar(node) ) {
            Node varNode = NodeFactory.createBlankNode();
            Node varName = NodeFactory.createLiteralString(Var.alloc(node).getVarName());
            graph.add(varNode, V.var, varName);
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
