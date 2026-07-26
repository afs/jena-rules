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

import org.apache.jena.sparql.util.Symbol;
import org.seaborne.jena.srl.sys.SysSRL;

// Default : SysJenaRules.dftEngineType
public enum EngineType {
    // As code.
    SIMPLE("csimpleEngineType"),
    // Body as SELECT
    SIMPLE_SPARQL("srl:simpleSparqlBody"),
    // Rule as SPARQL Update INSERT
    SIMPLE_SPARQL_INSERT("srl:simpleSparqlInsert"),
    // Rule as SPARQL CONSTRUCT
    SIMPLE_SPARQL_CONSTRUCT("srl:simpleSparqConstruct"),
    // Backwards evaluation.
    BKD_NON_RECURSIVE("srl:backwardNonRecursive");

    // This enum defined the names within jena-rules.
    // Symbols are "open ended enums" - extensions can register with the RulesEngineRegistry by using a symbol.

    private final Symbol symbol;

    EngineType(String string) {
        symbol = SysSRL.allocSymbol(string);
    }

    public Symbol symbol() { return symbol; }
}
