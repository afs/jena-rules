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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.seaborne.jena.shacl_rules.sys.ImportsProcessor;



public class TestImports {
    // Convert to manifest

    @Test public void imports_01() {
        RuleSet rs1 = ShaclRules.parseFile("src/test/files/imports/rs1.srl");

        assertTrue(rs1.hasImports());
        assertEquals(2, rs1.getImports().size());

        RuleSet rsx = ImportsProcessor.mergeClosure(rs1);

        assertEquals(4, rsx.getData().size());
        assertEquals(3, rsx.getRules().size());
    }

}
