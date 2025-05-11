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
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIxResolver;
import org.apache.jena.query.QueryException;
import org.apache.jena.shared.JenaException;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.core.Prologue;
import org.seaborne.jena.shacl_rules.lang.ElementRule;
import org.seaborne.jena.shacl_rules.lang.ShaclParseException;
import org.seaborne.jena.shacl_rules.lang.parser.ShaclRulesJavacc;

public class ShaclRulesParser {

    public static RuleSet parseString(String string) {
        Reader in = new StringReader(string);
        ShaclRulesJavacc parser = new ShaclRulesJavacc(in);
        return parse(parser, null);
    }

    public static RuleSet parseFile(String filename) {
        String base = IRILib.filenameToIRI(filename);
        return parseFile(filename, base);
    }

    public static RuleSet parseFile(String filename, String baseURI) {
        try (InputStream in = IO.openFileBuffered(filename)) {
            return parse(in, baseURI);
        } catch (IOException ex) {
            throw IOX.exception(ex);
        }
    }

    public static RuleSet parse(InputStream in , String baseURI) {
        ShaclRulesJavacc parser = new ShaclRulesJavacc(in);
        return parse(parser, baseURI);
    }

    // Parser to RuleSet
    private static RuleSet parse(ShaclRulesJavacc parser, String baseURI) {
        // XXX Change to prefix map.
        IRIxResolver resolver =
                (baseURI == null) ? IRIs.stdResolver().clone() : IRIs.resolver(baseURI);
        Prologue prologue = new Prologue(new PrefixMappingImpl(),resolver);

        parser.setPrologue(prologue);

        try {
            parser.RulesUnit();
            List<ElementRule> rulesParser = parser.getRules();
            // Translate to the abstract rule structure.
            List<Rule> rules = rulesParser.stream().map(elt->new Rule(elt.getHead().getList(), elt.getBody())).toList();
            List<Triple> triples = parser.getData();
            RuleSet ruleSet = new RuleSet(prologue, rules, triples);
            return ruleSet;
        } catch (org.seaborne.jena.shacl_rules.lang.parser.ParseException ex) {
            throw new ShaclParseException(ex.getMessage(), ex.currentToken.beginLine, ex.currentToken.beginColumn);
        } catch (org.seaborne.jena.shacl_rules.lang.parser.TokenMgrError tErr) {
            // Last valid token : not the same as token error message - but this should not happen
            int col = parser.token.endColumn;
            int line = parser.token.endLine;
            throw new ShaclParseException(tErr.getMessage(), line, col);
        } catch (QueryException ex) {
            throw ex;
        } catch (JenaException ex) {
            throw new ShaclParseException(ex.getMessage(), ex, -1, -1);
        } catch (Error err) {
            // The token stream can throw errors.
            throw new ShaclParseException(err.getMessage(), err, -1, -1);
        } catch (Throwable th) {
            Log.warn(ShaclRulesParser.class, "Unexpected throwable: ", th);
            int col = parser.token.endColumn;
            int line = parser.token.endLine;
            throw new ShaclParseException(th.getMessage(), th, line, col);
        }
    }


}
