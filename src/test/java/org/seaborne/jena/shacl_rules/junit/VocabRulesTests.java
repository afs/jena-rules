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

package org.seaborne.jena.shacl_rules.junit;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

public class VocabRulesTests {
    public static final String SRT =  "http://www.w3.org/ns/shacl-rules-test#";

    public static final Node TestPositiveSyntaxRules    = uri("RulesPositiveSyntaxTest");
    public static final Node TestNegativeSyntaxRules    = uri("RulesNegativeSyntaxTest");

    //public static final Node TestPositiveEvalRules      = uri("RulesPositiveEvalTest");
    public static final Node TestPositiveEvalRules      = uri("RulesEvalTest");
    public static final Node TestNegativeEvalRules      = uri("RulesNegativeEvalTest");

    public static final Node TestSurpressed             = uri("Test");

    public static final Node ruleSet                    = uri("ruleset");

    public static final Node data                       = uri("data");

    private static Node uri(String localName) { return uri(SRT, localName); }
    private static Node uri(String namespace, String localName) { return NodeFactory.createURI(namespace+localName); }


}