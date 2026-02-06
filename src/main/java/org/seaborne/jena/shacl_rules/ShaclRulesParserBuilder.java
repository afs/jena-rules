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
import java.io.StringReader;
import java.nio.file.Path;

import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.streammgr.StreamManager;
import org.apache.jena.sparql.util.Context;
import org.seaborne.jena.shacl_rules.lang.ShaclRulesSyntax;

/*package*/ class ShaclRulesParserBuilder {

    private String              filenameOrURI = null;
    private Path                filePath  = null;
    private String              stringToParse = null;
    private InputStream         inputStream = null;
    private StringReader        javaReader = null;
    private StreamManager       streamManager = null;
    private ErrorHandler        errorHandler = null;
    private String baseURI = null;
    private ShaclRulesSyntax rulesSyntax = null;
    private Context context = null;


    ShaclRulesParserBuilder() {}

    public static ShaclRulesParserBuilder create() { return new ShaclRulesParserBuilder() ; }

    public ShaclRulesParserBuilder fromString(String string) { this.stringToParse = string; return this; }
    public ShaclRulesParserBuilder from(String filenameOrURI) { this.filenameOrURI = filenameOrURI ; return this; }
    public ShaclRulesParserBuilder from(Path filePath) {  this.filePath = filePath ; return this; }
    public ShaclRulesParserBuilder from(InputStream inputStream) { this.inputStream = inputStream ;return this; }

    /**
     * Set the StreamManager to use when opening a URI (including files by name, but not by {@code Path}).
     * @param streamManager
     * @return this
     */
    public ShaclRulesParserBuilder streamManager(StreamManager streamManager) {
        this.streamManager = streamManager;
        return this;
    }

    public ShaclRulesParserBuilder errorHandler(ErrorHandler errorhandler) {
        this.errorHandler = errorhandler;
        return this;
    }

    public ShaclRulesParserBuilder baseURI(String baseURI) {
        this.baseURI = baseURI;
        return this;
    }

    public ShaclRulesParserBuilder syntax(ShaclRulesSyntax rulesSyntax) {
        this.rulesSyntax = rulesSyntax;
        return this;
    }

    public ShaclRulesParser build() {
        ShaclRulesSyntax syntax = this.rulesSyntax;
        if ( syntax == null )
            syntax = ShaclRulesParser.defaultRulesSyntax;

        return new ShaclRulesParser(filenameOrURI, filePath, stringToParse,
                                    inputStream, javaReader, streamManager,
                                    baseURI, errorHandler, syntax, context);
    }

    public RuleSet parse() { return build().parse(); }
}
