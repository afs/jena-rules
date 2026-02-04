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

package org.seaborne.jena.shacl_rules.lang.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.IOX;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.lang.ShaclRulesSyntax;
import org.seaborne.jena.shacl_rules.lang.parser.jena_rules.ParserJenaRules;
import org.seaborne.jena.shacl_rules.lang.parser.shacl_rules.ParserShaclRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclRulesParser0 {

    public final static Logger parserLogger = LoggerFactory.getLogger(ShaclRulesParser0.class);

    private static final ShaclRulesSyntax defaultRuleSyntax = ShaclRulesSyntax.JENA;

    /** Parse from a string, and return a {@link RuleSet}
     * @param string
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseString(String string) {
        return parseString(string, null, defaultRuleSyntax);
    }

    /**
     * Parse from a string and return a {@link RuleSet}
     * @param string
     * @param rulesSyntax
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseString(String string, ShaclRulesSyntax rulesSyntax) {
        return parseString(string, null, rulesSyntax);
    }

    /**
     * Parse from a string and return a {@link RuleSet}
     * @param string
     * @param baseURI
     * @param rulesSyntax
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseString(String string, String baseURI, ShaclRulesSyntax rulesSyntax) {
        StringReader strReader = new StringReader(string);
        return parse(strReader, baseURI, rulesSyntax);
    }

    /**
     * Parse a file or web document, and return a {@link RuleSet}
     * @param filenameOrURI
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseFile(String filenameOrURI) {
        return parseFile(filenameOrURI, null, defaultRuleSyntax);
    }

    /**
     * Parse a file, with given baseURI, and return a {@link RuleSet}.
     * @param filenameOrURI or URI
     * @param baseURI
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseFile(String filenameOrURI, String baseURI) {
        return parseFile(filenameOrURI, baseURI, defaultRuleSyntax);
    }

    /**
     * Parse from file or web document and return a {@link RuleSet}
     * @param filenameOrURI
     * @param baseURI
     * @param rulesSyntax
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseFile(String filenameOrURI, String baseURI, ShaclRulesSyntax rulesSyntax) {
        try (InputStream in = IO.openFileBuffered(filenameOrURI)) {
            return parse(in, baseURI, rulesSyntax);
        } catch (IOException ex) {
            throw IOX.exception(ex);
        }
    }

    /**
     * Parse from an {@code InputStream}
     * @param input
     * @param baseURI
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parse(InputStream input, String baseURI) {
        return parse(input, baseURI, defaultRuleSyntax);
    }

    public static RuleSet parse(InputStream in, String baseURI, ShaclRulesSyntax rulesSyntax) {
        return switch (rulesSyntax) {
            case SHACL->ParserShaclRules.parse(in, baseURI);
            case JENA->ParserJenaRules.parse(in, baseURI);
            default -> { throw new IllegalArgumentException("Syntax"); }
        };
    }

    private static RuleSet parse(StringReader strReader, String baseURI, ShaclRulesSyntax rulesSyntax) {
        return switch (rulesSyntax) {
            case SHACL->ParserShaclRules.parse(strReader, baseURI);
            case JENA->ParserJenaRules.parse(strReader, baseURI);
            default -> { throw new IllegalArgumentException("Syntax"); }
        };
    }
}
