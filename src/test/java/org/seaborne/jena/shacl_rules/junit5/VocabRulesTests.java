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

package org.seaborne.jena.shacl_rules.junit5;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

class VocabRulesTests {
    public static final String NS =  "https://jena.apache.org/rules#";
    public static final Resource TestPositiveSyntaxRules    = ResourceFactory.createResource( NS+"TestRulesPositiveSyntax" );
    public static final Resource TestNegativeSyntaxRules    = ResourceFactory.createResource( NS+"TestRulesNegativeSyntax" );
    public static final Resource TestEvalRules              = ResourceFactory.createResource( NS+"TestRulesEval" );
    public static final Resource TestNegativeEvalRules      = ResourceFactory.createResource( NS+"TestRulesNegativeEval" );
    public static final Resource TestSurpressed             = ResourceFactory.createResource( NS+"Test" );
}