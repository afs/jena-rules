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

package org.seaborne.jena.shacl_rules.sys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;

public class RuleDependencies {
    /** Return the rules that have a given rule depends on. */
    public static Collection<Rule> dependencies(Rule rule, RuleSet ruleSet) {
        List<Rule> array = new ArrayList<>();
        for ( Rule r : ruleSet.getRules() ) {
            for ( Triple bodyTriple : rule.getDependentTriples() ) {
                if ( dependsOn(bodyTriple, r) ) {
                    array.add(r);
                    // Short circuit looking in the rest of the body.
                    break;
                }
            }
        }
        return array;
    }

//    private static boolean dependsOn(Rule rule1, Rule rule2) {
//        List<Triple> inputTriples = rule1.getBody().getTriples();
//        List<Triple> outputTriples = rule2.getHead().getTriples();
//        for ( Triple input : inputTriples ) {
//            if ( mayProvide(input, rule2) )
//                return true;
//            }
//        return false;
//    }

    /**
     * Does {@code triple1} provide (generate triples) for {@code triple2}?
     * <p>
     * Triple ?s :p ?o "mayProvide" for triple ?s :p 123, or :s ?Q ?Z.
     * <p>In other words, can triple1, as a pattern, generate a concrete triple that triple2 matches?
     *
     */
    public static boolean dependsOn(Triple triple, Rule rule) {
        for(Triple headTriple : rule.getTripleTemplates() ) {
            if ( dependsOn(triple, headTriple) )
                return true;
        }
        return false;
    }

    /**
     * Does {@code triple1} provide (generate triples) for {@code triple2}?
     * <p>
     * Triple ?s :p ?o "mayProvide" for triple ?s :p 123, or :s ?Q ?Z.
     * <p>In other words, can triple1, as a pattern, generate a concrete triple that triple2 matches?
     *
     */
    public // For development.
    /*package*/ static boolean dependsOn(Triple triple1, Triple triple2) {
        // Does not consider variable names: e.g. ?x :p ?x
        // but safely returns true as "may provide".
        // Common case: concrete predicates - put first.
        return matches(triple1.getPredicate(), triple2.getPredicate()) &&
               matches(triple1.getSubject(), triple2.getSubject()) &&
               matches(triple1.getObject(), triple2.getObject());
    }

    /**
     * Either node1 is a variable, or node2 is a variable
     */
    private static boolean matches(Node node1, Node node2) {
        if ( Var.isVar(node1) )
            return true;
        if ( Var.isVar(node2) )
            return true;
        return node1.equals(node2);
    }

//    /**
//     * Either node1 is a variable (node2 is a variable or concrete), or concretely matches node2 (i.e. node2 is not a variable)
//     */
//    private static boolean isGeneralizationOf(Node node1, Node node2) {
//        // Order dependent
//        if ( Var.isVar(node1) )
//            return true;
//        return node1.equals(node2);
//    }
}
