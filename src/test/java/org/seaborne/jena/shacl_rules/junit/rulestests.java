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

package org.seaborne.jena.shacl_rules.junit;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.arq.junit.manifest.TestMakers;
import org.apache.jena.arq.junit.textrunner.TextTestRunner;

public class rulestests {

    // See rdftests
    public static void main(String[] args) {
        if ( args.length == 0 )
            args = new String[] {"src/test/files/manifest-rules.ttl"};
        List<String> manifests = Arrays.asList(args);
        TestMakers.system();
        TestMakers.install(RuleTests::makeRuleTest);
        TextTestRunner.run(manifests);
    }
}
