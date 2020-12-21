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

package org.seaborne.jena.rules.seminaive;

import java.util.Set;
import java.util.stream.Collectors ;

import org.apache.jena.graph.Node;
import org.seaborne.jena.rules.*;

/**
 * The <a href="">semi naïve</a> algorithm.
 *
 * This alogorithm tracks changes durign each pass over the rules and determines
 * whether on the next round, a rule needs to be attempted. If no rels of
 * involved in the body of the rule are generated at stage N-1, there is no need
 * to evaluate a rule on stage N.
 */
public class RuleEngineSemiNaive implements RuleEngine {
//    public static boolean DEBUG = false; 
    
    private final RelStore data; // RelStore.setReadOnly.
    private final RuleSet rules;

    public RuleEngineSemiNaive(RelStore data, RuleSet rules) {
        this.data = data;
        this.rules = rules;
    }

    @Override
    public RelStore exec(RuleExecCxt rCxt) {
        RelStore  generation = RelStoreFactory.createMem();
        generation.add(data);
        RelStore accLast = null ;
        int i = 0;
        while(true) {
            i++;
            if ( rCxt.debug() )
                System.out.printf("S: Round %d\n", i);
            RelStore acc = RelStoreFactory.createMem();
            for ( Rule rule : rules.asList() ) {
                boolean needed = false;
                if ( accLast == null ) {
                    needed = true ;
                } else {
                    // Var predicate.
                    Set<Node> predicates = accLast.get("").map(r->r.getTuple().get(1)).filter(p->!p.isVariable()).collect(Collectors.toSet());
                    for ( Rel r : rule.getBody() ) {
                        String name = r.getName();
                        if ( name.isEmpty() ) {
                            // RDF Predicate : the unnamed.
                            // what about (?s ?p ?o) ?
                            // For new triples of other parts are stable.
                            Node n = r.getTuple().get(1);
                            needed = predicates.contains(n);
                        } else {
                            needed = accLast.containRel(r.getName());
                        }
                        // Special case evaluation of (_,_,_)
                        if ( needed ) {
                            break ;
                        }
                        //System.out.printf("Not needed: %s in %s\n",r, rule);
                    }
                }
                if ( needed ) {
                    if ( rCxt.debug() )
                        System.out.println("==== "+rule);
                    RelStore data2 = RelStoreFactory.combine(generation, acc);
                    RuleExecLib.evalOne(rCxt, data2, acc, rule);
                } else {
                    //if ( rCxt.debug() )
                    //    System.out.println("Skip: "+rule);
                }
            }
            // Changes?
            if ( acc.isEmpty() ) {
                // Diffs
                return generation;
            }
            generation.add(acc);
            accLast = acc ;
        }
    }
}

