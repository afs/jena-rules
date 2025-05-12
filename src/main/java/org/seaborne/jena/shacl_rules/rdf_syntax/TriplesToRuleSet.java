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
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.graph.GNode;
import org.apache.jena.sparql.util.graph.GraphList;
import org.apache.jena.system.G;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;

public class TriplesToRuleSet {

    public static RuleSet parse(Graph graph) {

        // XXX Need identified data.
        RuleSet ruleSet ;
        List<Rule> rules = new ArrayList<>();

        List<Node> ruleNodes = G.listPO(graph, V.TYPE, V.ruleClass);

        ruleNodes.forEach(n->{
            Rule r = parseRule(graph, n);
            if ( r != null )
                rules.add(r);
        });

        Prologue prologue = new Prologue(graph.getPrefixMapping());
        return new RuleSet(prologue, rules, null);
    }

    private static Rule parseRule(Graph graph, Node n) {

        Node headNode = G.getOneSP(graph, n, V.head);
        Node bodyNode = G.getOneSP(graph, n, V.body);

        List<Triple> headTemplate = new ArrayList<>();


        GNode gNode = GNode.create(graph, headNode);
        List<Node> x = GraphList.members(gNode);
        x.forEach(node->{
            Node sn = G.getOneSP(graph, node, V.subject);
            Node pn = G.getOneSP(graph, node, V.predicate);
            Node on = G.getOneSP(graph, node, V.object);
            Node s = extractVar(graph, sn);
            Node p = extractVar(graph, pn);
            Node o = extractVar(graph, on);
            Triple t = Triple.create(s, p, o);
            headTemplate.add(t);
        });

        ElementGroup body = new ElementGroup();
        Rule rule = new Rule(headTemplate, body);
        return rule;
    }

    private static Node extractVar(Graph graph, Node node) {
        if ( ! node.isBlank() )
            return node;
        Node x = G.getZeroOrOneSP(graph, node, V.var);
        if ( x == null )
            throw new ShaclException("Blank node in pattern or malformed [ sh:var ]");
        return Var.alloc(x.getLiteralLexicalForm());
    }

}
