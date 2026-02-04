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

import java.io.InputStream;

import org.apache.jena.graph.Graph;
import org.seaborne.jena.shacl_rules.exec.EngineType;
import org.seaborne.jena.shacl_rules.exec.RuleSetEvaluation;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;

/**
 * Common operations.
 * <p>
 * @see ShaclRulesParser
 * @see ShaclRulesWriter
 * @see ShaclRulesExec
 */

public class ShaclRules {

    // -- Execute

    /**
     * Calculate the inference graph; this includes the data triples from the rule set.
     */
    public static Graph inferenceGraph(Graph graph, RuleSet ruleSet) {
        return evaluation(graph, ruleSet).inferredTriples();
    }

    /**
     * Calculate the output graph which includes the triples of the input graph,
     * the triples declared in the rule set and the inference graph.
     */
    public static Graph outputGraph(Graph graph, RuleSet ruleSet) {
        return evaluation(graph, ruleSet).outputGraph();
    }

    /**
     * {@link RuleSetEvaluation}
     */
    public static RuleSetEvaluation evaluation(Graph graph, RuleSet ruleSet) {
        return RulesEngine.create(EngineType.SIMPLE, graph, ruleSet).eval();
    }

    // -- Parse

    /** Parse from a string, and return a {@link RuleSet}
     * @param string
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseString(String string) {
        return ShaclRulesParser.fromString(string).parse();
    }

//    /**
//     * Parse from a string and return a {@link RuleSet}
//     * @param string
//     * @param rulesSyntax
//     * @returns RuleSet
//     * @throws ShaclRulesParseException
//     */
//    public static RuleSet parseString(String string, ShaclRulesSyntax rulesSyntax) {
//        return ShaclRulesParser.fromString(string).syntax(rulesSyntax).parse();
//    }
//
//    /**
//     * Parse from a string and return a {@link RuleSet}
//     * @param string
//     * @param baseURI
//     * @param rulesSyntax
//     * @returns RuleSet
//     * @throws ShaclRulesParseException
//     */
//    public static RuleSet parseString(String string, String baseURI, ShaclRulesSyntax rulesSyntax) {
//        return ShaclRulesParser.fromString(string).baseURI(baseURI).syntax(rulesSyntax).parse();
//    }

    /**
     * Parse a file or web document, and return a {@link RuleSet}
     * @param filenameOrURI
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseFile(String filenameOrURI) {
        return ShaclRulesParser.from(filenameOrURI).parse();
    }

    /**
     * Parse a file, with given baseURI, and return a {@link RuleSet}.
     * @param filenameOrURI or URI
     * @param baseURI
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseFile(String filenameOrURI, String baseURI) {
        return ShaclRulesParser.from(filenameOrURI).baseURI(baseURI).parse();
    }

//    /**
//     * Parse from file or web document and return a {@link RuleSet}
//     * @param filenameOrURI
//     * @param baseURI
//     * @param rulesSyntax
//     * @returns RuleSet
//     * @throws ShaclRulesParseException
//     */
//    public static RuleSet parseFile(String filenameOrURI, String baseURI, ShaclRulesSyntax rulesSyntax) {
//        return ShaclRulesParser.from(filenameOrURI).baseURI(baseURI).syntax(rulesSyntax).parse();
//    }

    /**
     * Parse from an {@code InputStream}
     * @param input
     * @param baseURI
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parse(InputStream input, String baseURI) {
        return ShaclRulesParser.from(input).baseURI(baseURI).parse();
    }

//    public static RuleSet parse(InputStream input, String baseURI, ShaclRulesSyntax rulesSyntax) {
//        return ShaclRulesParser.from(input).baseURI(baseURI).syntax(rulesSyntax).parse();
//    }
}
