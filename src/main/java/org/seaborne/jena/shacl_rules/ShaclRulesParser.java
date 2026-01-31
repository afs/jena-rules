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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.IOX;
import org.seaborne.jena.shacl_rules.lang.ShaclRulesSyntax;
import org.seaborne.jena.shacl_rules.lang.parser.jena_rules.ParserJenaRules;
import org.seaborne.jena.shacl_rules.lang.parser.shacl_rules.ParserShaclRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclRulesParser {

    public final static Logger parserLogger = LoggerFactory.getLogger(ShaclRulesParser.class);

    private static final ShaclRulesSyntax defaultRuleSyntax = ShaclRulesSyntax.SHACL;

    public static RuleSet parseString(String string) {
        return parseString(string, defaultRuleSyntax);
    }

    public static RuleSet parseFile(String filename) {
        return parseFile(filename, defaultRuleSyntax);
    }

    public static RuleSet parseFile(String filename, String baseURI) {
        return parseFile(filename, baseURI, defaultRuleSyntax);
    }

    public static RuleSet parse(InputStream in, String baseURI) {
        return parse(in, baseURI, defaultRuleSyntax);
    }

    // hacky
    // Make parse interface InputStream or Reader and have setup here.
    // BiFunction<InputStream, String, RuleSet>
    // BiFunction<Reader, String, RuleSet> - no! Need named functions.

    @FunctionalInterface
    private interface ParseInputStream { RuleSet parse(InputStream input); }

    @FunctionalInterface
    private interface ParseStringReader { RuleSet parse(StringReader input); }


    public static RuleSet parseString(String string, ShaclRulesSyntax rulesSyntax) {
        return switch (rulesSyntax) {
            case SHACL->ParserShaclRules.parseString(string);
            case JENA->ParserJenaRules.parseString(string);
            default-> { throw new IllegalArgumentException("Syntax"); }
        };
    }

    public static RuleSet parseFile(String filename, ShaclRulesSyntax rulesSyntax) {
        return switch (rulesSyntax) {
            case SHACL->ParserShaclRules.parseFile(filename);
            case JENA->ParserJenaRules.parseFile(filename);
            default-> { throw new IllegalArgumentException("Syntax"); }
        };
    }

    public static RuleSet parseFile(String filename, String baseURI, ShaclRulesSyntax rulesSyntax) {
        try (InputStream in = IO.openFileBuffered(filename)) {
            return parse(in, baseURI, rulesSyntax);
        } catch (IOException ex) {
            throw IOX.exception(ex);
        }
    }

    public static RuleSet parse(InputStream in, String baseURI, ShaclRulesSyntax rulesSyntax) {
        return switch (rulesSyntax) {
            case SHACL->ParserShaclRules.parse(in, baseURI);
            case JENA->ParserJenaRules.parse(in, baseURI);
            default -> { throw new IllegalArgumentException("Syntax"); }
        };
    }
}
