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

package org.seaborne.jena.srl;

import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import org.apache.jena.arq.junit.Scripts;
import org.apache.jena.arq.junit.manifest.TestMaker;
import org.seaborne.jena.srl.exec.EngineType;
import org.seaborne.jena.srl.junit.RuleTests;

@TestMethodOrder(OrderAnnotation.class)
public class Scripts_RuleEvalByEngine {

    @BeforeAll
    public static void beforeClass() {}

    @AfterAll
    public static void afterClass() {}

    private static TestMaker testMakerEngineType(EngineType engineType) {
        TestMaker r = manifestEntry ->  RuleTests.makeRuleTestByEngine(manifestEntry, engineType);
        return r ;
    }

    @Order(1)
    @TestFactory
    @DisplayName("Jena Rules (Execution - engine type : Simple)")
    public Stream<DynamicNode> execution_simple() {
        return Scripts.manifestTestFactory("src/test/files/eval/manifest.ttl", testMakerEngineType(EngineType.SIMPLE));
    }

    @Order(2)
    @TestFactory
    @DisplayName("Jena Rules (Execution - engine type : Simple SPARQL Body)")
    public Stream<DynamicNode> execution_simpleSparqlBody() {
        return Scripts.manifestTestFactory("src/test/files/eval/manifest.ttl", testMakerEngineType(EngineType.SIMPLE_SPARQL));
    }

    @Order(3)
    @TestFactory
    @DisplayName("Jena Rules (Execution - engine type : Simple SPARQL CONSTRUCT)")
    public Stream<DynamicNode> execution_simpleSparqlConstruct() {
        return Scripts.manifestTestFactory("src/test/files/eval/manifest.ttl", testMakerEngineType(EngineType.SIMPLE_SPARQL_CONSTRUCT));
    }

    @Order(4)
    @TestFactory
    @DisplayName("Jena Rules (Execution - engine type : Simple SPARQL INSERT)")
    public Stream<DynamicNode> execution_simpleSparqlInsert() {
        return Scripts.manifestTestFactory("src/test/files/eval/manifest.ttl", testMakerEngineType(EngineType.SIMPLE_SPARQL_INSERT));
    }


}
