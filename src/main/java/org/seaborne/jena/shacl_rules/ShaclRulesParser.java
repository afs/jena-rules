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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.riot.system.streammgr.StreamManager;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.shacl_rules.lang.ShaclRulesSyntax;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;
import org.seaborne.jena.shacl_rules.lang.parser.jena_rules.ParserJenaRules;
import org.seaborne.jena.shacl_rules.lang.parser.shacl_rules.ParserShaclRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclRulesParser {

    public final static Logger parserLogger = LoggerFactory.getLogger(ShaclRulesParser.class);

    static final ShaclRulesSyntax defaultRulesSyntax = ShaclRulesSyntax.JENA;

    public static ShaclRulesParserBuilder create() {
        return ShaclRulesParserBuilder.create();
    }

    public static ShaclRulesParserBuilder fromString(String string) { return create().fromString(string); }
    public static ShaclRulesParserBuilder from(String filenameorURI) { return create().from(filenameorURI); }
    public static ShaclRulesParserBuilder from(InputStream input) { return create().from(input); }

    private final String              filenameOrURI;
    private final Path                path;
    private final String              stringToParse;
    private final InputStream         inputStream;
    private final StringReader        javaReader;
    private final StreamManager       streamManager;
    private final String              baseURI;

    // Accept choice by the application
//    private final HttpClient          httpClient; // The httpClient might be provided by the RDFParserBuilder, but it might also be null
    //private final boolean             strict;
//    private final ErrorHandler        errorHandler;
    private final Context             context;
    private final ShaclRulesSyntax rulesSyntax;

    // Some cases the parser is reusable (read a file), some are not (input streams).
    private boolean                   canUseThisParser = true;


    /*package*/ ShaclRulesParser(String filenameOrURI, Path path, String content,
                                 InputStream inputStream, StringReader javaReader,
                                 StreamManager streamManager,
                                 String baseURI,
                                 ShaclRulesSyntax rulesSyntax,
                                 Context context
            ) {
        int x = countNonNull(filenameOrURI, path, content, inputStream, javaReader);
        if ( x >= 2 )
            throw new IllegalArgumentException("Only one source allowed: one of uri, path, content, inputStream and javaReader must be set");
        if ( x < 1 )
            throw new IllegalArgumentException("No source specified allowed: one of uri, path, content, inputStream and javaReader must be set");
        Objects.requireNonNull(rulesSyntax);

        this.filenameOrURI = filenameOrURI;
        this.path = path;
        this.stringToParse = content;
        this.inputStream = inputStream;
        this.javaReader = javaReader;
        this.streamManager = streamManager;
        this.baseURI = baseURI;
        this.rulesSyntax = rulesSyntax;
        this.context = context;
    }

    /**
    * @throws ShaclRulesParseException
    */
    public RuleSet parse() {
        if ( !canUseThisParser )
            throw new RulesException("Parser has been used once and can not be used again");
        // Consuming mode.
        canUseThisParser = (inputStream == null && javaReader == null);
        StringReader jr = javaReader;
        if ( stringToParse != null )
            jr = new StringReader(stringToParse);

        if ( inputStream != null )
            return parseInputStream(inputStream, baseURI, rulesSyntax);

        if ( jr != null )
            return parseJavaReader(jr, baseURI, rulesSyntax);

        if ( filenameOrURI != null ) {
            try ( InputStream in = IO.openFileBuffered(filenameOrURI) ) {
                return parseInputStream(in, baseURI, rulesSyntax);
            } catch (IOException ex) {
                IO.exception(ex);
            }
        }
        throw new RulesException("No source");
    }

    // XXX Packaged convenience functions. May go into a different class.

    /** Parse from a string, and return a {@link RuleSet}
     * @param string
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseString(String string) {
        return fromString(string).parse();
    }

    /**
     * Parse from a string and return a {@link RuleSet}
     * @param string
     * @param rulesSyntax
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseString(String string, ShaclRulesSyntax rulesSyntax) {
        return fromString(string).syntax(rulesSyntax).parse();
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
        return fromString(string).baseURI(baseURI).syntax(rulesSyntax).parse();
    }

    /**
     * Parse a file or web document, and return a {@link RuleSet}
     * @param filenameOrURI
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseFile(String filenameOrURI) {
        return from(filenameOrURI).parse();
    }

    /**
     * Parse a file, with given baseURI, and return a {@link RuleSet}.
     * @param filenameOrURI or URI
     * @param baseURI
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parseFile(String filenameOrURI, String baseURI) {
        return from(filenameOrURI).baseURI(baseURI).parse();
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
        return from(filenameOrURI).baseURI(baseURI).syntax(rulesSyntax).parse();
    }

    /**
     * Parse from an {@code InputStream}
     * @param input
     * @param baseURI
     * @returns RuleSet
     * @throws ShaclRulesParseException
     */
    public static RuleSet parse(InputStream input, String baseURI) {
        return parse(input, baseURI, defaultRulesSyntax);
    }

    public static RuleSet parse(InputStream in, String baseURI, ShaclRulesSyntax rulesSyntax) {
        return from(in).baseURI(baseURI).syntax(rulesSyntax).parse();
    }

    private static RuleSet parseJavaReader(StringReader jr, String baseURI, ShaclRulesSyntax rulesSyntax) {
        return switch (rulesSyntax) {
            case SHACL->ParserShaclRules.parse(jr, baseURI);
            case JENA->ParserJenaRules.parse(jr, baseURI);
            default -> { throw new IllegalArgumentException("Syntax"); }
        };
    }

    static RuleSet parseInputStream(InputStream in, String baseURI, ShaclRulesSyntax rulesSyntax) {
        return switch (rulesSyntax) {
            case SHACL->ParserShaclRules.parse(in, baseURI);
            case JENA->ParserJenaRules.parse(in, baseURI);
            default -> { throw new IllegalArgumentException("Syntax"); }
        };
    }

    /** Count the nulls */
    private static int countNonNull(Object... objs) {
        int x = 0;
        for ( Object obj : objs )
            if ( obj != null )
                x++;
        return x;
    }

    /** One or more non-null */
    private static boolean isNonNull(Object... objs) {
        int x = 0;
        for ( Object obj : objs )
            if ( obj != null )
                return true;
        return false;
    }

    /** All null */
    private static boolean allNull(Object... objs) {
        int x = 0;
        for ( Object obj : objs )
            if ( obj != null )
                return false;
        return true;
    }


    // Convenience.

}

