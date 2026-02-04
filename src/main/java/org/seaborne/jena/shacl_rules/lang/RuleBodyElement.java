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

public sealed interface RuleBodyElement  {
    public record EltTriplePattern(Triple triplePattern) implements RuleBodyElement {}
    public record EltNegation(List<RuleBodyElement> inner) implements RuleBodyElement {}
    public record EltCondition(Expr condition) implements RuleBodyElement {}
    public record EltAssignment(Var var, Expr expression) implements RuleBodyElement {}
//    public record EltAggregation(Var var, Expr expression) implements RuleElement {}
}