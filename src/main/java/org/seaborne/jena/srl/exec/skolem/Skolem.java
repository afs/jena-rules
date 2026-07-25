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

package org.seaborne.jena.srl.exec.skolem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jena.atlas.io.StringWriterI;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterNT;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.modify.TemplateLib;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.sparql.util.VarUtils;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.lang.RuleHeadElement.EltTripleTemplate;
import org.seaborne.jena.srl.lang.RuleHeadElement.EltTupleTemplate;
import org.seaborne.jena.srl.tuples.Tuple;
import org.seaborne.jena.srl.tuples.Tuples;

public class Skolem {


    /**
     * Get the head vars in encounter order.
     * The collection iterator is guaranteed to be iterate in the same order each time.
     */
    public
    //private
    static SequencedCollection<Var> getHeadVars(Rule rule) {
        // The head template is often small (10's of variables would be a lot) so using a list is also practical.
        LinkedHashSet<Var> uniqueVars = new LinkedHashSet<>();
        rule.getHeadElements().forEach(elt->{
            switch(elt) {
                case EltTripleTemplate(Triple tripleTemplate) -> {
                    VarUtils.addVarsFromTriple(uniqueVars, tripleTemplate);
                }
                case EltTupleTemplate(Tuple tupleTemplate) -> {
                    Tuples.addVars(uniqueVars, tupleTemplate);
                }
//                case null -> {}
//                default -> {}
            }
        });

        return uniqueVars;
//        // not necessary
//        List<Var> vars = new ArrayList<>(uniqueVars) {};
//        return vars;
    }


    // Per ruleSet map.

    // Also a formatter for toString use!
    static NodeFormatter formatter = new NodeFormatterNT();
    Map<String, BNodeProc> blankNodeAlloc = new ConcurrentHashMap<>();

    static Node alloc(int ruleId, Binding row, List<Var> vars) {
        String key = skolemKey(ruleId, vars, row);
        return null;
    }

    // Or a record.
    record SkolemKey(int ruleId, Binding row) {}


    private static String skolemKey(int ruleId, Iterable<Var> vars, Binding row) {
        try ( StringWriterI w = new StringWriterI() ) {
            w.write(Integer.toString(ruleId));
            for ( var v : vars ) {
                Node n = row.get(v);
                if ( n == null )
                    throw new InternalErrorException("Expected a binding for "+v);
                w.write("|");
                w.write("?");
                w.write(v.getVarName());
                w.write("|");
                formatter.format(w, n);
                //w.write(NodeFmtLib.displayStr(n));
            }
            return w.toString();
        }
    }




    static class BNodeProc {
        private final int ruleId;
        private final String skolemKey;
        final ConcurrentHashMap<Node, Node> mapper;

        BNodeProc(int ruleId,String skolemKey) {
            this.ruleId = ruleId ;
            this.skolemKey = skolemKey;
            this.mapper = new ConcurrentHashMap<>();
        }

        Node alloc(int ruleId, Binding row, List<Var> vars) {
            return null;
        }
    }



    // For RuleExecLib.templateInstantiationTriples


    // Per rule

    private static Iterator<Triple> templateInstantiationTriples0(List<Triple> headElements, Binding binding) {
        return TemplateLib.calcTriples(headElements, Iter.singletonIterator(binding));
    }

    static Map<String, BNodeProc> blankNodeMappers = new ConcurrentHashMap<>();

    private static Iterator<Triple> ruleHeadInstantiationTriples(int ruleId, Rule rule, List<Triple> headElements, Binding row) {
        SequencedCollection<Var> vars = getHeadVars(rule);
        return ruleHeadInstantiationTriples(ruleId, vars, headElements, row);
    }

    //private
    public static Iterator<Triple> ruleHeadInstantiationTriples(int ruleId, SequencedCollection<Var> vars, List<Triple>headElements, Binding row) {
        String skolemKey = skolemKey(ruleId, vars, row);
        BNodeProc bNodeProc = blankNodeMappers.computeIfAbsent(skolemKey, k -> new BNodeProc(ruleId, k));
        List<Triple> tripleList = new ArrayList<>(headElements.size());
        for ( Triple triple : headElements ) {
            Triple groundTriple = TemplateLib.subst(triple, row, bNodeProc.mapper);
            // Validity check.
            if ( !groundTriple.isConcrete() || ! NodeUtils.isValidAsRDF(groundTriple.getSubject(), groundTriple.getPredicate(), groundTriple.getObject()) ) {
                Log.warn(TemplateLib.class, "Unbound in triple: "+FmtUtils.stringForTriple(triple)) ;
                continue;
            }
            tripleList.add(groundTriple);
        }
        return tripleList.iterator();
    }

    // --- Template Lib
    /** Substitute into a triple, with rewriting of bNodes */
    public static Triple subst(Triple triple, Binding b, Map<Node, Node> bNodeMap) {
        // Transfer to quads.
        Node s = triple.getSubject();
        Node p = triple.getPredicate();
        Node o = triple.getObject();

        Node s1 = subst(s, b, bNodeMap, true);
        Node p1 = subst(p, b, bNodeMap, false);
        Node o1 = subst(o, b, bNodeMap, true);

        if ( s1 == s && p1 == p && o1 == o )
            return triple;
        return Triple.create(s1, p1, o1);
    }

    // Deep node processing, with blank node rewrite and variable substitution.
    public static Node subst(Node n, Binding b, Map<Node, Node> bNodeMap, boolean allowTripleTerms) {
        if ( n.isURI() || n.isLiteral() )
            return n;
        if ( Var.isVar(n))
            return Substitute.substitute(Var.alloc(n), b);
        if ( n.isBlank() || Var.isBlankNodeVar(n) )
            return newBlank(n, bNodeMap);
        if ( allowTripleTerms && n.isTripleTerm() ) {
            Triple t1 = subst(n.getTriple(), b, bNodeMap);
            return NodeFactory.createTripleTerm(t1);
        }
        return n;
    }

    /** Generate a blank node consistently */
    private static Node newBlank(Node n, Map<Node, Node> bNodeMap) {
        Node n2 = bNodeMap.get(n);
        if ( n2 != null )
            return n2;
        Node bNew = NodeFactory.createBlankNode();
        bNodeMap.put(n, bNew);
        return bNew;
    }
    // --- Template Lib

}

