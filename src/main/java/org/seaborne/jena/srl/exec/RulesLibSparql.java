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
import org.apache.jena.sparql.core.*;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.update.Update;
import org.seaborne.jena.srl.Rule;
import org.seaborne.jena.srl.RuleBody;
import org.seaborne.jena.srl.lang.RuleBodyElement;
import org.seaborne.jena.srl.lang.RuleBodyElement.*;
import org.seaborne.jena.srl.lang.parser.SynAggregationAssignment;

/**
 * Support for translation of SPARQL-RL Rules into executable SPARQL query or update.
 * The objective is functional correctness.
 */
public class RulesLibSparql {

    /** Return a translation of a {@link RuleBody} as a {@link ElementGroup} syntax element. */
    public static ElementGroup ruleBodyToElementGroup(RuleBody ruleBody) {
        // Used for translating GROUP with no aggregates.
        VarAlloc varAlloc = new VarAlloc(varAllocBase);
        return ruleBodyToElementGroup(ruleBody.getBodyElements(), ruleBody.isGrounded(), varAlloc);
    }

    /** Return a translation of a {@link RuleBody} as a SPARQL SELECT * Query. */
    public static Query ruleBodyToSelectQuery(RuleBody ruleBody) {
        var eltGroup = ruleBodyToElementGroup(ruleBody);
        Query query = new Query();
        query.setQuerySelectType();
        query.setQueryResultStar(true);
        query.setQueryPattern(eltGroup);
        return query;
    }

    /** Return a translation of a {@link Rule} (rule head and rule body) as a SPARQL CONSTRUCT Query. */
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

    /** Return a translation of a {@link Rule} (rule head and rule body) as a SPARQL INSERT Update */
    public static Update ruleToInsert(Rule rule) {
        Element eltGroup = RulesLibSparql.ruleBodyToElementGroup(rule.getBody());
        UpdateModify insert = new UpdateModify();
        insert.setElement(eltGroup);
        QuadAcc quadAcc = insert.getInsertAcc();
        rule.getHead().getHeadTriples().forEach(quadAcc::addTriple);
        return insert;
    }

    private static String varAllocBase = "__genvar_";
    // Or we don't need portable SPARQL:
    // private static String varAllocBase = ARQConstants.allocQueryVariables+"srlVar";

    private static ElementGroup ruleBodyToElementGroup(List<RuleBodyElement> ruleElts, boolean groundedPattern, VarAlloc varAlloc) {
        // Only EltTriplePattern with ElementGroup.addTriplePattern changes
        // a element of added to "group", other operations use addElement to append
        // new elements.
        // An EltGroup shallow copies this list into its input sub-query,
        ElementGroup group = new ElementGroup();

        for ( RuleBodyElement rElt : ruleElts ) {
            switch (rElt) {
                // The SPARQL Parser generates ElementPathBlock but ElementTriplesBlock works fine.
                case EltTriplePattern(var triple) -> group.addTriplePattern(triple);
                case EltTuplePattern(var tuple) -> { throw new NotImplemented("Tuples in SPARQL translation"); }
                case EltFilter(var expr) -> group.addElement(new ElementFilter(expr));
                case EltNegation(var innerBody, boolean grounded ) -> {
                    // NOT GROUND handled by ruleBodyToElementGroup which will wrap the innerBody in a GRAPH <baseURI>
                    ElementGroup inner = ruleBodyToElementGroup(innerBody, grounded, varAlloc);
                    Element negationElt = new ElementFilter(new E_NotExists(inner));
                    group.addElement(negationElt);

                    // Example: RULE {...} WHERE { ?s :p :o . NOT { ?s :q ?y } . ?y :r 123 }
                    // SPARQL moves filter to the end-of-group. Need to stop FILTER moving right by SPARQL rules.
                    // Create a new accumulating group with the previous group as its first element.
                    // (A new well-formedness rule may disallow use of ?y after the NOT inner body.)
                    ElementGroup group2 = new ElementGroup();
                    group2.addElement(group);
                    group = group2;
                }
                case EltAssignment(var assignedVar, var expr) -> {
                    // set(?x) is bind(?x) filter(bound(?x))
                    group.addElement(new ElementBind(assignedVar, expr));
                    Expr v = new ExprVar(assignedVar);
                    Expr bound = new E_Bound(v);
                    group.addElement(new ElementFilter(bound));
                }
                case EltGroup(List<Var> groupKeys, List<RuleBodyElement> pattern, List<SynAggregationAssignment> aggAssignments) -> {
                    // GROUP, no keys : make a special case?

                    boolean emptyGroupKeys = (groupKeys == null || groupKeys.isEmpty() );

                    // Only when there are group keys. A keyless input sub-query has nothing to
                    // project; the projection-less sub-SELECT loses its project in the algebra and
                    // the input pattern's variables leak into the group pattern, multiplying the
                    // aggregate by the number of input rows. The outer join already restricts.
                    Query inputQuery = null;
                    if ( group.size() > 0 && ! emptyGroupKeys ) {
                        Query inputQueryX = new Query();
                        inputQueryX.setQuerySelectType();
                        // Shallow copy
                        ElementGroup eg = new ElementGroup();
                        eg.getElements().addAll(group.getElements());
                        inputQueryX.setQueryPattern(eg);
                        groupKeys.forEach(var -> {
                            inputQueryX.addResultVar(var);
                        });
                        inputQueryX.ensureResultVars();
                        inputQueryX.setDistinct(true);
                        inputQuery = inputQueryX;
                    }

                    ElementGroup inner = ruleBodyToElementGroup(pattern, false, varAlloc);
                    if ( inputQuery != null )
                        inner.getElements().add(0, new ElementSubQuery(inputQuery));

                    Query innerQuery = new Query();
                    innerQuery.setQuerySelectType();
                    innerQuery.setQueryPattern(inner);
                    if ( ! emptyGroupKeys ) {
                        groupKeys.forEach(var -> {
                            innerQuery.addGroupBy(var);     // GROUP BY ?var
                            innerQuery.addResultVar(var);   // SELECT ?var
                            });
                    }

                    // No aggregate? Put in a dummy.
                    if ( aggAssignments.isEmpty() && emptyGroupKeys ) {
                        // Dummy aggregate
                        Var v = varAlloc.allocVar();
                        Aggregator agg = AggregatorFactory.createCount(false);
                        // SAMPLE better?
                        // Different query?
                        Expr exprAgg = innerQuery.allocAggregate(agg) ;
                        innerQuery.addResultVar(v, exprAgg);
                    } else {
                        aggAssignments.forEach(synAgg ->{
                            Var v = synAgg.aggAssignVar();
                            Expr e = synAgg.aggExpr();
                            Aggregator agg = switch(synAgg.aggFunction()) {
                                case COUNT -> { yield (e == null) ? AggregatorFactory.createCount(false) : AggregatorFactory.createCountExpr(false,  e); }
                                case COUNT_DISTINCT -> { yield (e == null) ? AggregatorFactory.createCount(true) : AggregatorFactory.createCountExpr(true,  e); }
                                case MAX -> AggregatorFactory.createMax(false, e);
                                case MIN -> AggregatorFactory.createMin(false, e);
                                case SUM -> AggregatorFactory.createSum(false, e);
                            };
                            Expr exprAgg = innerQuery.allocAggregate(agg) ;
                            innerQuery.addResultVar(v, exprAgg);
                        });
                    }
                    innerQuery.ensureResultVars();
                    ElementSubQuery eltSubQuery = new ElementSubQuery(innerQuery);
                    group.addElement(eltSubQuery);

                    // By well-formedness, Group keys can't be unbound after the sub-query is joined to the input.
//                    // This is not necessary:
//                    if ( groupKeys != null ) {
//                        for ( Var var : groupKeys ) {
//                            Expr v = new ExprVar(var);
//                            Expr bound = new E_Bound(v);
//                            group.addElement(new ElementFilter(bound));
//                        }
//                    }
                    for ( var synAgg : aggAssignments ) {
                        Expr v = new ExprVar(synAgg.aggAssignVar());
                        Expr bound = new E_Bound(v);
                        group.addElement(new ElementFilter(bound));
                    }
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

    /*package*/ static DatasetGraph buildDataset(Graph evalGraph, Graph inputGraph) {
        DatasetGraph dsg = DatasetGraphFactory.createGeneral(evalGraph);
        if ( inputGraph != null )
            dsg.addGraph(EvalConst.srlBaseDataGraph, inputGraph);
        return dsg;
    }

}
