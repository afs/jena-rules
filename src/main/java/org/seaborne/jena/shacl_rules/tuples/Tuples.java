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

package org.seaborne.jena.shacl_rules.tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.sse.*;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;

public class Tuples {


    public static void print(TupleStore tupleStore, PrefixMap prefixMap) {
        TupleStoreWriter.print(tupleStore, prefixMap);
    }

    public static void printSSE(TupleStore tupleStore, PrefixMap prefixMap) {
        NodeFormatter nFmt =  new NodeFormatterTTL(null, prefixMap);
        try ( IndentedWriter iOut = IndentedWriter.stdout.clone() ) {
            iOut.print("(tuples");
            iOut.incIndent();

            for ( Tuple tuple : tupleStore ) {
                printTuple(iOut, tuple, nFmt);
            }
            iOut.decIndent();
            iOut.println(")");
        }
    }

    private static void printTuple(IndentedWriter iOut, Tuple tuple, NodeFormatter nFmt) {
        iOut.print("( ");
        boolean first = true;
        for ( Node n : tuple ) {
            if ( ! first )
                iOut.print(" ");
            else
                first = false;
            nFmt.format(iOut, n);
        }
        iOut.print(" )");
        iOut.println();
    }


    public static TupleStore tupleStoreSSE(String str) {
        List<Tuple> elts = parseSSE(str);
        return new TupleStoreSimple(elts);
    }

    public static List<Tuple> parseSSE(String str) {
        Item item = SSE.parseItem(str);
        if ( ! item.isList() )
            throw new SSE_Exception("Not a list of tuples");
        if ( item.isTagged("tuples") )
            return buildTuplesUntagged(item.getList(), 1);
        else
            return buildTuplesUntagged(item.getList(), 0);
    }

    /** SRL syntax */
    public static List<Tuple> parse(String str) {
        RuleSet ruleSet = ShaclRulesParser.parseString(str);
        if ( ! ruleSet.hasData() )
            throw new RulesException("List of tuples: data present");
        if ( ! ruleSet.hasImports() )
            throw new RulesException("List of tuples: imports present");
        if ( ! ruleSet.getRules().isEmpty() )
            throw new RulesException("List of tuples: rulesPresent");
        return ruleSet.getDataTuples();
    }

    private static List<Tuple> buildTuplesUntagged(ItemList list, int idx) {
        List<Tuple> tuples = new ArrayList<>();

        for ( int eltIdx = idx ; eltIdx < list.size() ; eltIdx++ ) {
            Item elt = list.get(eltIdx);
            if ( ! elt.isList() )
                throw new SSE_Exception("Item "+eltIdx+" : not a list");
            Tuple tuple = buildTuple(elt.getList());
            tuples.add(tuple);
        }
        return tuples;
    }

    private static Tuple buildTuple(ItemList list) {
        List<Node> nodes = new ArrayList<>();
        for ( int i = 0 ; i < list.size() ; i++ ) {
            Item elt = list.get(i);
            Item nodeItem = ItemLift.liftItem(elt);
            if ( ! nodeItem.isNode() )
                throw new SSE_Exception("Not a node : "+nodeItem);
            nodes.add(nodeItem.getNode());
        }
        return Tuple.create(nodes);
    }

    public static Tuple substitute(Tuple tuple, Binding binding) {
        if ( isNotNeeded(binding) )
            return tuple;
        if ( tuple.isConcrete() )
            return tuple;
        int N = tuple.size();
        Node[] terms = new Node[N];
        boolean changed = false;
        for ( int i = 0 ; i < N ; i++ ) {
            Node n1 = tuple.get(i);
            // This deals with triple terms.
            Node n2 = Substitute.substitute(n1, binding);
            if ( ! n1.sameTermAs(n2) )
                changed = true;
            terms[i] = n2;
        }
        if ( ! changed )
            return tuple;
        return Tuple.create(terms);
    }

    public static Tuple substituteTemplate(Tuple tuple, Binding binding, Map<Node, Node> bNodeMap) {
        if ( isNotNeeded(binding) )
            return tuple;
        if ( tuple.isConcrete() )
            return tuple;
        int N = tuple.size();
        Node[] terms = new Node[N];
        boolean changed = false;
        for ( int i = 0 ; i < N ; i++ ) {
            Node n1 = tuple.get(i);
            if ( n1.isBlank() ) {
                changed = true;
                n1 = newBlank(n1, bNodeMap);
            }
            // This deals with triple terms.
            Node n2 = Substitute.substitute(n1, binding);
            if ( ! n1.sameTermAs(n2) )
                changed = true;
            terms[i] = n2;
        }
        if ( ! changed )
            return tuple;
        return Tuple.create(terms);
    }

    //DRY: TemplateLib
    /** generate a blank node consistently */
    private static Node newBlank(Node n, Map<Node, Node> bNodeMap) {
        if ( !bNodeMap.containsKey(n) )
            bNodeMap.put(n, NodeFactory.createBlankNode());
        return bNodeMap.get(n);
    }


    private static boolean isNotNeeded(Binding b) {
        return b == null || b.isEmpty();
    }

    public static String displayStr(Tuple tuple) {
        StringJoiner sj = new StringJoiner(" ", "$(", ")");
        for ( Node n : tuple )
            sj.add(NodeFmtLib.displayStr(n));
        return sj.toString();
    }

    // Development helper
    public static Tuple createTuple(String...strings) {
        Node[] terms = new Node[strings.length];
        for ( int i = 0 ; i < strings.length ; i++ ) {
            terms[i] = SSE.parseNode(strings[i]);
        }
        return Tuple.create(terms);
    }
}
