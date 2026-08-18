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

package org.seaborne.jena.srl.exec;

import java.util.List;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.update.Update;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleBody;
import org.seaborne.jena.srl.lang.RuleBodyElement;
import org.seaborne.jena.srl.lang.RuleBodyElement.*;

/** This class is not API */
public class RulesLibSparql {

    /** RuleElements (i.e. a rule body) to a (SPARQL syntax) {@link ElementGroup}. */
    public static ElementGroup ruleBodyToElementGroup(RuleBody ruleBody) {
        return ruleBodyToElementGroup(ruleBody.getBodyElements(), ruleBody.isGrounded());
    }

    private static ElementGroup ruleBodyToElementGroup(List<RuleBodyElement> ruleElts, boolean groundedPattern) {
        ElementGroup group = new ElementGroup();
        ElementTriplesBlock tBlk = null;
        for ( RuleBodyElement rElt : ruleElts ) {
            switch (rElt) {
                case EltTriplePattern(var triple) -> group.addTriplePattern(triple);
                case EltTuplePattern(var tuple) -> { throw new NotImplemented("Tuples in SPARQL translation"); }
                case EltFilter(var expr) -> group.addElement(new ElementFilter(expr));
                case EltNegation(var innerBody, boolean grounded ) -> {
                    // NOT DATA handled by ruleBodyToElementGroup which will wrap the innerBody in a GRAPH <

                    ElementGroup inner = ruleBodyToElementGroup(innerBody, grounded);
                    Element negationElt = new ElementFilter(new E_NotExists(inner));
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
        if ( groundedPattern ) {
            Element eltGraph = new ElementNamedGraph(EvalConst.srlBaseDataGraph, group);
            ElementGroup eltGroup = new ElementGroup();
            eltGroup.addElement(eltGraph);
            group = eltGroup;
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
        if ( ! rule.getHeadTuples().isEmpty() )
            throw new NotImplemented("Tuples in SPARQL translation");
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


    /*package*/ static DatasetGraph buildDataset(Graph evalGraph, Graph inputGraph) {
        DatasetGraph dsg = DatasetGraphFactory.createGeneral(evalGraph);
        if ( inputGraph != null )
            dsg.addGraph(EvalConst.srlBaseDataGraph, inputGraph);
        return dsg;
    }

}
