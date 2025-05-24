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
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.sparql.util.ExprUtils;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.jena.JLib;
import org.seaborne.jena.shacl_rules.rdf_syntax.expr.SparqlNodeExpression;

public class RuleSetToTriples {

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
            ElementGroup body = rule.getBody();
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
        BasicPattern head = rule.getHead();
        List<Node> x = basicGraphPatternAsList(graph, head);
        Node bgpNode = list(graph, x);
        graph.add(ruleNode, V.head, bgpNode);
        return bgpNode;
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

        if ( false ) {
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
        Node x = SparqlNodeExpression.exprToRDF(graph, expr);

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

    private static List<Node> basicGraphPatternAsList(Graph graph, BasicPattern bgp) {
        List<Node> elements = new ArrayList<>();
        bgp.forEach(triple->{
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
            elements.add(tripleNode);
        });
        return elements;
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
