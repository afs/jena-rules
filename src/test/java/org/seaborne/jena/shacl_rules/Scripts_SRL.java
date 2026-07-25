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

package org.seaborne.jena.shacl_rules;

import java.util.stream.Stream;

import org.junit.jupiter.api.*;

import org.apache.jena.arq.junit.Scripts;
import org.seaborne.jena.shacl_rules.junit.RuleTests;

public class Scripts_SRL {

    // The WG test suite as it in in tests/ area.
    // 2026-07 -- Currently, there is significant overlap with the files/tests/tests-* so they run twice.
    // At this stage, better to be safe than miss tests.

    @BeforeAll
    public static void beforeClass() {}

    @AfterAll
    public static void afterClass() {}

    @TestFactory
    @DisplayName("SRL")
    public Stream<DynamicNode> execution() {
        // All SRL tests, using the system default query engine for eval tests.
        return Scripts.manifestTestFactory("src/test/files/manifest-rules.ttl", RuleTests::makeRuleTest);
    }
}
