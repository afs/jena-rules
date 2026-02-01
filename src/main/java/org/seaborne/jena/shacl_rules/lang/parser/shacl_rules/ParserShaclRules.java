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

package org.seaborne.jena.shacl_rules.lang.parser.shacl_rules;

import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIx;
import org.apache.jena.irix.IRIxResolver;
import org.apache.jena.riot.system.*;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.lang.parser.ParserRules;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesParseException;
import org.seaborne.jena.shacl_rules.lang.parser.shacl_rules.javacc.ParseException;
import org.seaborne.jena.shacl_rules.lang.parser.shacl_rules.javacc.ShaclRulesJavacc;
import org.seaborne.jena.shacl_rules.lang.parser.shacl_rules.javacc.TokenMgrError;
import org.slf4j.Logger;

// Class specific parser code
public class ParserShaclRules extends ParserRules {

    public static RuleSet parse(InputStream in , String baseURI) {
        ShaclRulesJavacc parser = new ShaclRulesJavacc(in);
        return parse(parser, baseURI);
    }

    public static RuleSet parse(StringReader strReader, String baseURI) {
        ShaclRulesJavacc parser = new ShaclRulesJavacc(strReader);
        return parse(parser, baseURI);
    }

     private final static Logger parserLogger = ShaclRulesParser.parserLogger;

    // Parser to RuleSet
    private static RuleSet parse(ShaclRulesJavacc parser, String baseURI) {
        IRIxResolver resolver =
                (baseURI == null) ? IRIs.stdResolver().clone() : IRIs.resolver(baseURI);

        // The rules parsing catches the triples by context.
        // PrefixMap is managed by the ParserProfile and the sent to the StreamRDF.
        StreamRDF output = StreamRDFLib.sinkNull();
        ErrorHandler errorHandler = new ErrorHandlerRuleParser(parserLogger);
        ParserProfile parserProfile =
                ParserRules.createParserProfile(RiotLib.factoryRDF(), errorHandler, resolver, true);

        parser.setDest(output);
        parser.setProfile(parserProfile);

        try {
            output.start();
            parser.RuleSet();
            output.finish();

            List<Rule> rules = parser.getRules();
            List<Triple> triples = parser.getData();

            // Last seen
            String declaredBaseURI = parser.getBaseIRI();
            IRIx baseIRI = (declaredBaseURI != null) ? IRIx.create(declaredBaseURI) : null;

            RuleSet ruleSet = RuleSet.create(baseIRI, parserProfile.getPrefixMap(), parser.getImports(), rules, triples);
            return ruleSet;
        }
        catch (ParseException ex) {
            parserProfile.getErrorHandler().error(ex.getMessage(), ex.currentToken.beginLine, ex.currentToken.beginColumn);
            throw new ShaclRulesParseException(ex.getMessage(), ex.currentToken.beginLine, ex.currentToken.beginColumn);
        }
        catch (TokenMgrError tErr) {
            // Last valid token : not the same as token error message - but this should not happen
            int col = parser.token.endColumn;
            int line = parser.token.endLine;
            parserProfile.getErrorHandler().error(tErr.getMessage(), line, col);
            throw new ShaclRulesParseException(tErr.getMessage(), line, col);
        }
   }
}
