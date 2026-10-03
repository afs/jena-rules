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

package org.seaborne.jena.srl;

import java.io.InputStream;

import org.apache.jena.graph.Graph;
import org.apache.jena.irix.IRIs;
import org.apache.jena.sparql.SystemARQ;
import org.apache.jena.sparql.util.Symbol;
import org.seaborne.jena.srl.exec.RuleSetEvaluation;
import org.seaborne.jena.srl.lang.RulesSyntax;
import org.seaborne.jena.srl.lang.parser.SRLParseException;
import org.seaborne.jena.srl.sys.P;
import org.seaborne.jena.srl.sys.SysJenaRules;
import org.seaborne.jena.srl.tuples.TupleStore;

/**
 * Common operations.
 * <p>
 * @see SRLParser
 * @see SRLWriter
 * @see SRLExec
 */

public class SRL {

    // XXX Check name.
    public static String mtShapeRuleLanguage = "application/shape-rules";

    public static String symbolNS = P.JenaRulesSymbolsNS;;
    public static Symbol symStrict = SystemARQ.allocSymbol(symbolNS, "strict");

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
        return SRLExec.create(SysJenaRules.dftEngineType, graph, ruleSet).eval();
    }

    /**
     * {@link RuleSetEvaluation}
     */
    public static RuleSetEvaluation evaluation(Graph graph, TupleStore inputTupleStore, RuleSet ruleSet) {
        return SRLExec.create(SysJenaRules.dftEngineType, graph, inputTupleStore, ruleSet).eval();
    }

    // -- Parse

    /** Parse from a string, and return a {@link RuleSet}
     * @param string
     * @return RuleSet
     * @throws SRLParseException
     */
    public static RuleSet parseString(String string) {
        return SRLParser.fromString(string).parse();
    }

  /**
  * Parse from a string and return a {@link RuleSet}
  * @param string
  * @param rulesSyntax
  * @return RuleSet
  * @throws SRLParseException
  */
    public static RuleSet parseString(String string, RulesSyntax rulesSyntax) {
        return SRLParser.fromString(string).syntax(rulesSyntax).parse();
    }

    /**
     * Parse a file or web document, and return a {@link RuleSet}
     * @param filenameOrURI
     * @throws SRLParseException
     */
    public static RuleSet parseFile(String filenameOrURI) {
        String base = IRIs.resolve(filenameOrURI);
        return SRLParser.from(filenameOrURI).baseURI(base).parse();
    }

    /**
     * Parse a file, with given baseURI, and return a {@link RuleSet}.
     * @param filenameOrURI or URI
     * @param baseURI
     * @return RuleSet
     * @throws SRLParseException
     */
    public static RuleSet parseFile(String filenameOrURI, String baseURI) {
        return SRLParser.from(filenameOrURI).baseURI(baseURI).parse();
    }

    /**
     * Parse from an {@code InputStream}
     * @param input
     * @param baseURI
     * @return RuleSet
     * @throws SRLParseException
     */
    public static RuleSet parse(InputStream input, String baseURI) {
        return SRLParser.from(input).baseURI(baseURI).parse();
    }
}
