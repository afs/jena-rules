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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.seaborne.jena.shacl_rules.Rule;

public class RuleDependencies0 {

    // See DependencyGraph.edges
//    /**
//     * Return the rules that have a given rule depends on.
//     */
//    public static Collection<Edge> dependencies_SUSPECT(Rule rule, RuleSet ruleSet) {
//        // See DependencyGraph2
//        // BETTER For each triple in the rule body, scan for provider.
//        List<Edge> array = new ArrayList<>();
//        for ( Rule r : ruleSet.getRules() ) {
//            accumulateEdges(array, rule, Link.POSITIVE, r, r.getBodyElements());
//        }
//        return array;
//    }

//    private static void accumulateEdges(List<Edge> array, Rule rule, Link link, Rule r, List<RuleElement> elts) {
//        for ( RuleElement elt : elts ) {
//            Edge edge = possibleEdge(array, rule, elt, r);
//            if ( edge != null )
//                array.add(edge);
//            // XXX Can stop looking if negative or aggrgeate.
//        }
//    }
//
//    // XXX Suspect.
//    private static Edge possibleEdge(List<Edge> array, Rule rule, RuleElement elt, Rule r) {
//        switch(elt) {
//            case RuleElement.EltTriplePattern(Triple triplePattern) -> {
//                if ( dependsOn(triplePattern, r) ) {
//                  return new Edge(rule, Link.POSITIVE, r);
//              }
//            }
//            case RuleElement.EltNegation(List<RuleElement> inner) -> {
//                // Anything inside NOT is also "negative"
//                accumulateEdges(array, rule, Link.NEGATIVE, r, inner);
//            }
//            default -> {}
//        }
//        return null;
//    }

    // ---- END

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
     * Does {@code triple} provide (generate triples) for {@code triple2}?
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
     * <p>In other words, can triple1, as a pattern, generate a concrete triple that triple2 as a template can produce?
     *
     */
    public // For development.
    /*package*/ static boolean dependsOn(Triple triplePattern, Triple tripleTemplate) {
        // Does not consider variable names: e.g. ?x :p ?x
        // but safely returns true as "may provide".
        // Common case: concrete predicates - put first.
        return matches(triplePattern.getPredicate(), tripleTemplate.getPredicate()) &&
               matches(triplePattern.getSubject(), tripleTemplate.getSubject()) &&
               matches(triplePattern.getObject(), tripleTemplate.getObject());
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
