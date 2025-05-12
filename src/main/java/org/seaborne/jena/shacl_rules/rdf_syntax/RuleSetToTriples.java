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
import org.apache.jena.graph.*;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;

public class RuleSetToTriples {

    static Node NIL = RDF.Nodes.nil;
    static Node CAR = RDF.Nodes.first;
    static Node CDR = RDF.Nodes.rest;
    static Node TYPE = RDF.Nodes.type;

    static class V {
        static final String SH = "http://www.w3.org/ns/shacl#";
        private static Node uri(String localName) { return NodeFactory.createURI(SH+localName); }

        static final Node subject = uri("subject");
        static final Node predicate = uri("predicate");
        static final Node object = uri("object");

        static final Node ruleClass = uri("Rule");
        static final Node head = uri("head");
        static final Node body = uri("body");

        static final Node rule = uri("rule");

        static final Node sparqlExpr = uri("sparqlExpr");

        // Temp
        static final Node sparqlBody = uri("sparqlBody");
    }

    /*
sh:rule
  sh:head ( [ sh:subject [ sh:var "x" ] ] ;
              sh:predicate rdf:type ;
              sh:object ex:Square ;
            ] );
  sh:body (
    [ sh:subject [ sh:var "x" ] ;
      sh:predicate ex:width ;
      sh:object [ sh:var "w" ]
    ]
    [ sh:subject [ sh:var "x" ] ;
      sh:predicate ex:height ;
      sh:object [ sh:var "h" ]
    ]
    [ sh:filter [ sparql:equals ( [ sh:var "w" ] [ sh:var "h" ] ) ] ]
  ) ;
  .
*/

    static int ruleNumber = 0;

    public static Graph write(RuleSet ruleSet) {
        Graph graph = GraphFactory.createDefaultGraph();
        graph.getPrefixMapping().setNsPrefix("sh", V.SH);
        graph.getPrefixMapping().setNsPrefix("rdf", RDF.getURI());
        graph.getPrefixMapping().setNsPrefix("ex", "http://example/");
        if ( ruleSet.hasData() ) {
            GraphUtil.add(graph, ruleSet.getDataTriples());
        }

        List<Node> rules = new ArrayList<>();

        ruleSet.getRules().forEach(rule->{
            ruleNumber++;
            Node ruleNode = NodeFactory.createURI("http://example/rule"+ruleNumber);
            if ( true ) {
                graph.add(ruleNode, TYPE, V.ruleClass);
            } else {
                Node x = NodeFactory.createBlankNode();
                graph.add(ruleNode, V.rule, x);
                ruleNode = x;
            }
            Node nHead = writeHead(graph, ruleNode, rule);
            ElementGroup body = rule.getBody();
            Node nBody = writeBody(graph, ruleNode, rule);
            rules.add(ruleNode);
        });
        return graph;
    }

    private static Node writeBody(Graph graph, Node ruleNode, Rule rule) {
        ElementGroup elg = rule.getBody();
        List<Node> items = new ArrayList<>();
        for ( Element e : elg.getElements() ) {
            switch(e) {
                case ElementTriplesBlock tBlk -> {
                    List<Node> x = basicGraphPatternAsList(graph, tBlk.getPattern());
                    items.addAll(x);
                }
                case ElementPathBlock pBlk -> {
                    BasicPattern bodyTriplePattern = new BasicPattern();
                    pBlk.getPattern().forEach(triplePath->{
                        // Better - sort out seq and alt.
                        Triple t = triplePath.asTriple();
                        if ( t == null )
                            throw new ShaclException("Path expression triples: "+triplePath);
                        bodyTriplePattern.add(t);
                    });
                    List<Node> x = basicGraphPatternAsList(graph, bodyTriplePattern);
                    items.addAll(x);
                }
                case ElementFilter fBlk -> {
                    Node nExpr = expression(graph, fBlk.getExpr());
                    items.add(nExpr);
                }
                default -> {
                    throw new ShaclException("Not supported for RDF output: "+e.getClass().getSimpleName());
                }
            }
        }

        if ( true ) {
            // Put in the SPARQL form.
            String qs = rule.getBody().toString();
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
      Node x = NodeFactory.createBlankNode();
      String exprStr = exprAsString(expr);
      Node obj = NodeFactory.createLiteralString(exprStr);
      graph.add(x, V.sparqlExpr, obj);
      return x;
    }

    private static String exprAsString(Expr expr) {
        IndentedLineBuffer out = new IndentedLineBuffer();
        ExprUtils.fmtSPARQL(out, expr);
        //WriterSSE.out(out, expr, null);
        return out.asString();
    }

    private static Node writeBody0(Graph graph, Node ruleNode, Rule rule) {
        String qs = rule.getBody().toString();
        Node bodyNode = NodeFactory.createBlankNode();
        Node nQueryString = NodeFactory.createLiteralString(qs);
        graph.add(bodyNode, V.sparqlBody, nQueryString);
        graph.add(ruleNode, V.body, bodyNode);
        return bodyNode;
    }

    private static Node writeHead(Graph graph, Node ruleNode, Rule rule) {
        BasicPattern head = rule.getHead();
        List<Node> x = basicGraphPatternAsList(graph, head);
        Node bgpNode = list(graph, x);
        graph.add(ruleNode, V.head, bgpNode);
        return bgpNode;
    }

    private static List<Node> basicGraphPatternAsList(Graph graph, BasicPattern bgp) {
        List<Node> elements = new ArrayList<>();
        bgp.forEach(triple->{
            Node tripleNode = NodeFactory.createBlankNode();
            Triple sTriple = Triple.create(tripleNode, V.subject, triple.getSubject());
            Triple pTriple = Triple.create(tripleNode, V.predicate, triple.getPredicate());
            Triple oTriple = Triple.create(tripleNode, V.object, triple.getObject());
            graph.add(sTriple);
            graph.add(pTriple);
            graph.add(oTriple);
            elements.add(tripleNode);
        });
        return elements;
    }

    private static Node list(Graph graph, List<Node> elements) {

        ListIterator<Node> iter = elements.listIterator(elements.size());
        Node x = NIL;

        while(iter.hasPrevious()) {
            Node cell = NodeFactory.createBlankNode();
            Node elt = iter.previous();
            graph.add(cell, CAR, elt);
            graph.add(cell, CDR, x);
            x = cell;
        }
        return x;
    }
}
