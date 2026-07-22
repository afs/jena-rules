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

package org.seaborne.jena.shacl_rules.exec;

import java.util.List;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.update.Update;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleBody;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;

/** This class is not API */
class RulesLibSparql {

    /** RuleElements (i.e. a rule body) to a (SPARQL syntax) {@link ElementGroup}. */
    public static ElementGroup ruleBodyToElementGroup(RuleBody ruleBody) {
        return ruleBodyToElementGroup(ruleBody.getBodyElements());
    }

    private static ElementGroup ruleBodyToElementGroup(List<RuleBodyElement> ruleElts) {
        ElementGroup group = new ElementGroup();
        ElementTriplesBlock tBlk = null;
        for ( RuleBodyElement rElt : ruleElts ) {
            switch (rElt) {
                case EltTriplePattern(var triple) -> group.addTriplePattern(triple);
                case EltTuplePattern(var tuple) -> { throw new NotImplemented(); }
                case EltFilter(var expr) -> group.addElement(new ElementFilter(expr));
                case EltNegation(var innerBody, boolean grounded ) -> {
                    Element inner = ruleBodyToElementGroup(innerBody);
                    Element negationElt = new ElementFilter(new E_NotExists(inner));
                    if ( grounded ) {
                        Node n = NodeFactory.createURI("arq:baseGraph");
                        negationElt = new ElementNamedGraph(n, negationElt);
                    }
                    group.addElement(negationElt);
                }
                case EltAssignment(var assignedVar, var expr) -> {
                    // set(?x) is bind(?x) filter(bound(?x))
                    group.addElement(new ElementBind(assignedVar, expr));
                    Expr v = new ExprVar(assignedVar);
                    Expr bound = new E_Bound(v);
                    group.addElement(new ElementFilter(bound));
                }
            }
        }
        return group;
    }

    /** RuleElements (i.e. a rule body) to a SPARQL SELECT Query. */
    public static Query ruleBodyToQuery(RuleBody ruleBody) {
        var eltGroup = ruleBodyToElementGroup(ruleBody);
        Query query = new Query();
        query.setQuerySelectType();
        query.setQueryResultStar(true);
        query.setQueryPattern(eltGroup);
        return query;
    }

    /** Rule(rule head and rule body) to a SPARQL CONSTRUCT Query. */
    public static Query ruleToConstruct(Rule rule) {
        Element elt = RulesLibSparql.ruleBodyToElementGroup(rule.getBody());
        Query query = new Query();
        query.setQueryPattern(elt);
        BasicPattern bgp = new BasicPattern(rule.getHeadTriples());
        Template template = new Template(bgp);
        query.setConstructTemplate(template);
        query.setQueryConstructType();
        return query;
    }


    /** Rule(rule head and rule body) to a SPARQL INSERT Update */
    public static Update ruleToInsert(Rule rule) {
        Element eltGroup = RulesLibSparql.ruleBodyToElementGroup(rule.getBody());
        UpdateModify insert = new UpdateModify();
        insert.setElement(eltGroup);
        QuadAcc quadAcc = insert.getInsertAcc();
        rule.getHead().getHeadTriples().forEach(quadAcc::addTriple);
        return insert;
    }


//    /** Get triple patters out of an ElementGroup built by {@link #ruleEltsToElementGroup} */
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
