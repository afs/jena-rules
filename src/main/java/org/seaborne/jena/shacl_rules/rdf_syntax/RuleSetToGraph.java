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
import org.apache.jena.sparql.graph.NodeConst;
import org.apache.jena.sparql.util.ExprUtils;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.jena.JenaLib;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.nexpr.SrlExpressions;
import org.seaborne.jena.shacl_rules.sys.P;
import org.seaborne.jena.shacl_rules.sys.SysJenaRules;
import org.seaborne.jena.shacl_rules.sys.V;
import org.seaborne.jena.shacl_rules.tuples.Tuple;

public class RuleSetToGraph {

    public static Graph asGraph(RuleSet ruleSet) {
        Graph graph = GraphFactory.createDefaultGraph();
        if ( ruleSet.hasPrefixMap() )
            graph.getPrefixMapping().setNsPrefixes(Prefixes.adapt(ruleSet.getPrefixMap()));
        P.basicPrefixes(graph);
        writeToGraph(graph, ruleSet);
        return graph;
    }

    public static void writeToGraph(Graph graph, RuleSet ruleSet) {
        Node ruleSetNode = NodeFactory.createURI("http://example/ruleSet-1");
        graph.add(ruleSetNode, V.TYPE, V.classRuleSet);

        // For each rule, write, add the blank node to a list.
        List<Node> rules = new ArrayList<>();
        ruleSet.getRules().forEach(rule->{
            Node ruleNode =  writeRule(graph, rule);
            rules.add(ruleNode);
        });
        Node rulesList = list(graph, rules);
        graph.add(ruleSetNode, V.rules, rulesList);

        writeData(graph, ruleSet, ruleSetNode);
        writeTupleData(graph, ruleSet, ruleSetNode);
    }

    // Write one rule.
    private static Node writeRule(Graph graph, Rule rule) {
        Node ruleNode;
        if ( rule.getId() != null ) {
            ruleNode = rule.getId();
        } else {
            ruleNode = NodeFactory.createBlankNode();
        }
        graph.add(ruleNode, V.TYPE, V.classRule);

        Node nHead = writeHead(graph, ruleNode, rule);
        graph.add(ruleNode, V.head, nHead);

        Node nBody = writeBody(graph, ruleNode, rule);
        graph.add(ruleNode, V.body, nBody);
        if ( rule.isGrounded() )
            graph.add(ruleNode, V.grounded, NodeConst.TRUE);
        return ruleNode;
    }

    private static boolean WriteAsTripleTerms = false;

    // Write data ("Axiomatic triple")
    private static void writeData(Graph graph, RuleSet ruleSet, Node ruleSetNode) {
        if ( ! ruleSet.hasData() )
                return;
            List<Triple> triples = ruleSet.getDataTriples();
            if ( WriteAsTripleTerms ) {
                // <<( ... )>>
                List<Node> tripleTerms = Iter.iter(triples).map(triple -> NodeFactory.createTripleTerm(triple)).toList();
                Node x = JenaLib.createList(graph, tripleTerms);
                graph.add(ruleSetNode, V.data, x);

            } else {
                // [ srl;subject ? ; srl:predicate ? ; srl:object ? ]
                List<Node> triplesNodes = Iter.iter(triples).map(triple -> encodeTriple(graph, triple)).toList();
                Node x = JenaLib.createList(graph, triplesNodes);
                graph.add(ruleSetNode, V.data, x);
            }
        }

    private static void writeTupleData(Graph graph, RuleSet ruleSet, Node ruleSetNode) {
        List<Tuple> tuples = ruleSet.getDataTuples();
        if ( tuples.isEmpty() )
            // No tuples? Nothing added the the graph
            return;

        List<Node> tupleData = new ArrayList<>();

        // For each tuple: write a list (no srl:tuple)

        tuples.forEach(tuple->{
            Node tx = tupleAsList(graph, tuple);
            tupleData.add(tx);
        });

        Node x = JenaLib.createList(graph, tupleData);
        graph.add(ruleSetNode, V.dataTuples, x);
    }

    private static Node writeHead(Graph graph, Node ruleNode, Rule rule) {
        List<RuleHeadElement> headElts = rule.getHeadElements();
        Node headNode = headElements(graph, headElts);
        return headNode;
    }

    private static Node headElements(Graph graph, List<RuleHeadElement> headElts) {
        List<Node> items = new ArrayList<>();
        for ( RuleHeadElement headElt : headElts ) {
            switch(headElt) {
                case RuleHeadElement.EltTripleTemplate(var tripleTemplate) -> {
                    if ( SysJenaRules.useRoleTriples ) {
                        Node encTriple = encodeTriple(graph, tripleTemplate);
                        Node x = attach(graph, V.tripleTemplate, encTriple);
                        items.add(x);
                    } else {
                        items.add(encodeTriple(graph, tripleTemplate));
                    }
                }
                case RuleHeadElement.EltTupleTemplate(var tupleTemplate) -> {
                    if ( SysJenaRules.useRoleTriples ) {
                        items.add(encodeTuple(graph, tupleTemplate));
                    } else {
                        Node encTuple = encodeTuple(graph, tupleTemplate);
                        Node x = attach(graph, V.tupleTemplate, encTuple);
                        items.add(x);
                    }
                }
                case null -> {}
                default -> {}

            }
        }
        Node headNode = list(graph, items);
        return headNode;
    }

    /** Create an attachment triple - this describes the role being played */
    private static Node attach(Graph graph, Node property, Node value) {
        Node blank = NodeFactory.createBlankNode();
        Triple elementTriple = Triple.create(blank, property, value);
        graph.add(elementTriple);
        return blank;
    }

    private static Node writeBody(Graph graph, Node ruleNode, Rule rule) {
        List<RuleBodyElement> bodyElts = rule.getBodyElements();
        Node bodyNode = writeBodyElements(graph, bodyElts);
        return bodyNode;
    }

    private static Node writeBodyElements(Graph graph, List<RuleBodyElement> bodyElts) {
        List<Node> items = new ArrayList<>();
        bodyElts.forEach(elt->{
            switch(elt) {
                case EltTriplePattern(var triplePattern) -> {
                    if ( SysJenaRules.useRoleTriples ) {
                        Node encTriple = encodeTriple(graph, triplePattern);
                        Node x = attach(graph, V.triplePattern, encTriple);
                        items.add(x);
                    } else {
                        items.add(encodeTriple(graph, triplePattern));
                    }
                }
                case EltTuplePattern(var tuplePattern) -> {
                    if ( SysJenaRules.useRoleTriples ) {
                        Node encTuple = encodeTuple(graph, tuplePattern);
                        Node x = attach(graph, V.tuplePattern, encTuple);
                        items.add(x);
                    } else {
                        items.add(encodeTuple(graph, tuplePattern));
                    }
                }
                case EltFilter(Expr condition) -> {
                    Node x1 = NodeFactory.createBlankNode();
                    Node nExpr = expression(graph, condition);
                    graph.add(x1, V.filter, nExpr);
                    items.add(x1);
                }
                case EltNegation(var innerBody, boolean grounded) ->{
                    // [NOT DATA]
                    Node nInnerBody = writeBodyElements(graph, innerBody);
                    Node x1 = NodeFactory.createBlankNode();
                    graph.add(x1, V.negation, nInnerBody);
                    if ( grounded )
                        graph.add(x1, V.grounded, NodeConst.TRUE);
                    items.add(x1);
                }

                case EltAssignment(var var, var expression) -> {
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

    // Expr to node expression, no srl:expr
    private static Node expression(Graph graph, Expr expr) {
        Node x = SrlExpressions.exprAsRDF(graph, expr);
        return x;
    }

    private static String exprAsString(Expr expr) {
        IndentedLineBuffer out = new IndentedLineBuffer();
        ExprUtils.fmtSPARQL(out, expr);
        //WriterSSE.out(out, expr, null);
        return out.asString();
    }

    /**
     * Encode a triple (pattern, template), updating the graph.
     * See also {@link #attach(Node, Node)}.
     */
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

    /**
     * Encode a tuple (pattern, template), updating the graph.
     * An encoded tuple is an RDF  list.
     * See also {@link #attach(Node, Node)}.
     */
    private static Node encodeTuple(Graph graph, Tuple tuple) {
        Node tupleNode = NodeFactory.createBlankNode();
        Node listTuples = tupleAsList(graph, tuple);
        return tupleNode;
    }

    private static Node tupleAsList(Graph graph, Tuple tuple) {
        List<Node> elts = tuple.terms();
        Node listTuples = JenaLib.listIntoGraph(elts, graph);
        return listTuples;
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
