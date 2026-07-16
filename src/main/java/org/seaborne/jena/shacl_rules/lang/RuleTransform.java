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

package org.seaborne.jena.shacl_rules.lang;

import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement.*;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement.*;
import org.seaborne.jena.shacl_rules.tuples.Tuple;

public interface RuleTransform {

    public default RuleHeadElement transform(EltTripleTemplate eltTripleTemplate, Triple tripleTemplate) { return null; }

    public default RuleHeadElement transform(EltTupleTemplate eltTupleTemplate, Tuple tupleTemplate) { return null; }

    public default RuleBodyElement transform(EltTriplePattern eltTriplePattern,Triple triplePattern) { return null; }

    public default RuleBodyElement transform(EltTuplePattern eltTuplePattern, Tuple tuplePattern) { return null; }

    public default RuleBodyElement transform(EltNegation eltNegation, List<RuleBodyElement> inner, boolean grounded) { return null; }

    public default RuleBodyElement transform(EltFilter eltFilter, Expr expr) { return null; }

    public default RuleBodyElement transform(EltAssignment eltAssignment, Var var, Expr value) { return null; }
}
