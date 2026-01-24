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

package org.seaborne.jena.shacl_rules;

import java.util.stream.Stream;

import org.junit.jupiter.api.*;

import org.apache.jena.arq.junit.Scripts;
import org.seaborne.jena.shacl_rules.junit.RuleTests;

public class Scripts_RuleEval {

    @BeforeAll
    public static void beforeClass() {}

    @AfterAll
    public static void afterClass() {}

    @TestFactory
    @DisplayName("Jena Rules (Execution)")
    public Stream<DynamicNode> execution() {
        return Scripts.manifestTestFactory("src/test/files/wellformed/manifest.ttl", RuleTests::makeRuleTest);
    }
}
