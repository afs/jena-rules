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

import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.seaborne.jena.shacl_rules.lang.RuleElement;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltAssignment;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltCondition;
import org.seaborne.jena.shacl_rules.lang.RuleElement.EltTriplePattern;

public class RuleLib {

    /** RuleElements (i.e. a rule body) to a (SPARQL syntax) {@link ElementGroup}. */
    public static ElementGroup ruleEltsToElementGroup(List<RuleElement> ruleElts) {
        ElementGroup group = new ElementGroup();
        ElementTriplesBlock tBlk = null;
        for ( RuleElement rElt : ruleElts ) {
            switch (rElt) {
                case EltTriplePattern(var triple) -> group.addTriplePattern(triple);
                case EltCondition(var expr) -> group.addElement(new ElementFilter(expr));
                case EltAssignment(var assignedVar, var expr) -> {
                    group.addElement(new ElementBind(assignedVar, expr));
                }
            }
        }
        return group;
    }

    /** RuleElements (i.e. a rule body) to a SPARQL Query. */
    public static Query ruleEltsToQuery(List<RuleElement> ruleElts) {
        var eltGroup = ruleEltsToElementGroup(ruleElts);
        Query query = new Query();
        query.setQuerySelectType();
        query.setQueryResultStar(true);
        query.setQueryPattern(eltGroup);
        return query;
    }

//    // Extract the triples from the body's Element Group.
//    // Needed for dependency analysis.
//    private static List<Triple> elementToTriples(ElementGroup eltGroup) {
//        List<Triple> triples = new ArrayList<>();
//
//        for ( Element e : eltGroup.getElements() ) {
//            switch(e) {
//                case ElementTriplesBlock tBlk -> {
//                    triples.addAll(tBlk.getPattern().getList());
//                }
//                case ElementPathBlock pBlk -> {
//                    BasicPattern bodyTriplePattern = new BasicPattern();
//                    pBlk.getPattern().forEach(triplePath->{
//                        // Better - sort out seq and alt.
//                        Triple t = triplePath.asTriple();
//                        if ( t == null )
//                            throw new ShaclException("Path expression triples: "+triplePath);
//                        triples.add(t);
//                    });
//                }
//                case ElementFilter fBlk -> {/*ignore*/}
//                default -> {
//                    throw new ShaclException("Not supported for RDF output: "+e.getClass().getSimpleName());
//                }
//            }
//        }
//        return Collections.unmodifiableList(triples);
//    }


}
