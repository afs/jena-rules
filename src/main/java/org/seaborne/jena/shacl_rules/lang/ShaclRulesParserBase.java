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

package org.seaborne.jena.shacl_rules.lang;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.TextDirection;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.riot.lang.extra.LangParserBase;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.Path;
import org.seaborne.jena.shacl_rules.Rule;

public class ShaclRulesParserBase extends LangParserBase {

    private List<Rule> rules = new ArrayList<>();
    private List<Triple> data = new ArrayList<>();

    public List<Rule> getRules() { return rules; }
    public List<Triple> getData() { return data; }

    private static boolean DEBUG = false;
    public static void debug(boolean setting) {
        DEBUG = setting;
    }

    private static void debug(String fmt, int line, int column, Object...args) {
        if ( DEBUG ) {
            String posn = String.format("[%2d, %2d] ", line, column);
            System.out.print(posn);
            String msg = String.format(fmt, args);
            System.out.println(msg);
        }
    }
    // The finish "start, end" arguments are position of the start of the syntax element

    // ---- Parser state.

    enum BuildState { NONE, OUTER, DATA, RULE, HEAD, BODY, INNER };
    protected BuildState state = BuildState.OUTER;

    // Used while parsing the head.
    private List<Triple> headAcc = null;
    // Used while parsing the body.
    private List<RuleElement> bodyAcc = null;
    // Used while parsing a negation or aggregation inner body.
    private List<RuleElement> innerBodyAcc = null;

    // ----

    protected void startRules() {
        state = BuildState.OUTER;
    }

    protected void finishRules() {
        if ( state != BuildState.OUTER )
            throwInternalStateException("finishRuleSet: Unfinished rule?");
        state = BuildState.NONE;
    }

    protected void startRule(int line, int column) {
        if ( bodyAcc != null )
            throwInternalStateException("startHead: Already in a rule");
        if ( headAcc != null )
            throwInternalStateException("startHead: Already in a rule");

        headAcc = new ArrayList<>();
        bodyAcc = new ArrayList<>();
        state = BuildState.RULE;
    }

    protected void finishRule(int line, int column) {
        if ( headAcc == null )
            throwInternalStateException("Null head");
        if ( bodyAcc == null )
            throwInternalStateException("Null body");

        Rule rule = Rule.create(headAcc, bodyAcc);

        headAcc = null;
        bodyAcc = null;
        rules.add(rule);
        // Data is accumulative through the parser run.
        state = BuildState.OUTER;
    }

    protected void startHead(int line, int column) {
        debug("startHead", line, column);
        state = BuildState.HEAD;
    }

    protected void finishHead(int line, int column) {
        debug("finishHead", line, column);
        state = BuildState.NONE;
    }

    protected void startBody(int line, int column) {
        debug("startBody", line, column);
        state = BuildState.BODY;
        bodyAcc = new ArrayList<>();
    }

    protected void finishBody(int line, int column) {
        debug("finishBody", line, column);
        state = BuildState.RULE;
    }

    protected void startBodyBasic(int line, int column) {
        debug("startBodyBasic", line, column);
        state = BuildState.INNER;
    }

    protected void finishBodyBasic(int line, int column) {
        debug("finishBodyBasic", line, column);
        state = BuildState.BODY;
    }

    // Allows variables. Paths expand by the parser (??)
    protected void startTriplesBlock(int line, int column) {
        debug("startTriplesBlock", line, column);
    }

    protected void finishTriplesBlock(int line, int column) {
        debug("finishTriplesBlock", line, column);
    }

    // Allows variables. Used for head.
    protected void startTriplesTemplate(int line, int column) {
        debug("startTriplesTemplate", line, column);
    }

    protected void finishTriplesTemplate(int line, int column) {
        debug("finishTriplesTemplate", line, column);
        // finishHead did the work.
    }

    // No variables, no paths
    protected void startData(int line, int column) {
        debug("startTriplesTemplate", line, column);
        state = BuildState.DATA;
    }

    protected void finishData(int line, int column) {
        debug("startTriplesTemplate", line, column);
        state = BuildState.OUTER;
    }

    protected void startNegation(int line, int column) {
        debug("startNegation", line, column);
        state = BuildState.INNER;
        innerBodyAcc = new ArrayList<>();
    }

    protected void finishNegation(int line, int column) {
        debug("finishNegation", line, column);
        state = BuildState.BODY;
    }

//    protected void startExistsElement(int line, int column) {
//        debug("startExistsElement", line, column);
//        state = BuildState.INNER;
//        innerBodyAcc = new ArrayList<>();
//    }
//
//    protected void finishExistsElement(int line, int column) {
//        debug("finishExistsElement", line, column);
////        RuleElement rElt = new RuleElement.EltExists(innerBodyAcc);
////        addToBody(rElt);
//        state = BuildState.BODY;
//    }
//
//    protected void startNotExistsElement(int line, int column) {
//        debug("startNotExistsElement", line, column);
//        state = BuildState.INNER;
//        innerBodyAcc = new ArrayList<>();
//    }
//
//    protected void finishNotExistsElement(int line, int column) {
//        debug("finishNotExistsElement", line, column);
////        RuleElement rElt = new RuleElement.EltNotExists(innerBodyAcc);
////        addToBody(rElt);
//        state = BuildState.BODY;
//    }

    private void addToHead(Triple tripleTemplate) {
        requireNonNull(tripleTemplate);
        headAcc.add(tripleTemplate);
    }

    private void addToBody(RuleElement ruleElt) {
        requireNonNull(ruleElt);
        switch(state) {
            case BODY -> bodyAcc.add(ruleElt);
            case INNER -> innerBodyAcc.add(ruleElt);
            default ->
                throwInternalStateException("Rule element emitted when in state "+state);
        }
    }

    // XXX Rename to the specific element type.

    // Triple pattern.
    private void addRuleElement(Triple triplePattern) {
        requireNonNull(triplePattern);
        addToBody(new RuleElement.EltTriplePattern(triplePattern));
    }

    // Condition
    private void addRuleElement(Expr expression) {
        requireNonNull(expression);
        addToBody(new RuleElement.EltCondition(expression));
    }

    // Negation
    private void addRuleElement(List<RuleElement> inner) {
        requireNonNull(inner);
        RuleElement rElt = new RuleElement.EltNegation(inner);
        addToBody(rElt);
}

    // Assignment
    private void addRuleElement(Var var, Expr expression) {
        requireNonNull(var);
        requireNonNull(expression);
        addToBody(new RuleElement.EltAssignment(var, expression));
    }

    protected void emitTriple(Node s, Node p, Path path, Node o, int line, int column) {
        debug("emitTriple", line, column);
        if ( path != null ) {
            if ( path instanceof P_Link pLink ) {
                p = pLink.getNode();
            } else {
                profile.getErrorHandler().error("Path - ignored", line, column);
                return;
            }
        }
        accTriple(s, p, o, line, column);
    }

    protected void emitTriple(Node s, Node p, Node o, int line, int column) {
        debug("emitTriple", line, column);
        accTriple(s, p, o, line, column);
    }

    // If NOT EXISTS, EXISTS
    protected void emitExistsElement(int line, int column) {
//      RuleElement rElt = new RuleElement.EltExists(innerBodyAcc);
//      addToBody(rElt);
        innerBodyAcc = null;
    }

    protected void emitNotExistsElement(int line, int column) {
//        RuleElement rElt = new RuleElement.EltNotExists(innerBodyAcc);
//        addToBody(rElt);
        innerBodyAcc = null;
    }

    protected void emitNegation(int line, int column) {
        addRuleElement(innerBodyAcc);
        innerBodyAcc = null;
    }

    protected void emitFilterExpr(Expr expr, int line, int column) {
        debug("emitFilterExpr", line, column);
        addRuleElement(expr);
    }

    protected void emitAssignment(Var var, Expr expr, int line, int column) {
        debug("emitAssignment", line, column);
        addRuleElement(var, expr);
    }

    protected Node emitTripleReifier(Node reifierId, Node s, Node p, Node o, int line, int column) {
        return super.emitTripleReifier(line, column, reifierId, s, p, o);
    }

    protected void transitiveProperty(String iriStr) {
        throw new NotImplemented("TRANSITIVE");
    }

    protected void symmetricProperty(String iriStr) {
        throw new NotImplemented("SYMMETRIC");
    }

    protected void inverseProperties(String iriStr1, String iriStr2) {
        throw new NotImplemented("INVERSE");
    }

    protected void declareImport(String iri) {
        throw new NotImplemented("IMPORTS");
    }

    // --

    protected void throwParseException(String msg, int line, int column) {
        throw new ShaclRulesParseException(msg, line, column);
    }

    protected void throwInternalStateException(String msg) {
        throw new IllegalStateException(msg);
    }

    private void accTriple(Node s, Node p , Node o, int line, int column) {
        Triple triple = Triple.create(s, p, o);

        switch(state) {
            case HEAD -> { addToHead(triple); }
            case BODY -> { addRuleElement(Triple.create(s,p,o)); }
            case INNER -> {
                Triple triplePattern = Triple.create(s,p,o);
                requireNonNull(triplePattern);
                innerBodyAcc.addLast(new RuleElement.EltTriplePattern(triplePattern));
            }
            case DATA -> {
                if ( ! triple.isConcrete() )
                    throwParseException("Triple must be concrete (no variables): "+triple, line, column);
                data.add(triple);
            }
            // Bad
//            case NONE -> {}
//            case OUTER -> {}
//            case RULE -> {}
            default -> {
                throwInternalStateException("Triple emitted in state "+state);
            }
        }
    }

    // To LangParseBase?
    // langParseBase out of "extra"

    // --- Universal base
    // From QueryParserBase

    protected Node stripSign(Node node) {
        if ( !node.isLiteral() )
            return node;
        String lex = node.getLiteralLexicalForm();
        String lang = node.getLiteralLanguage();
        RDFDatatype dt = node.getLiteralDatatype();

        if ( !lex.startsWith("-") && !lex.startsWith("+") )
            throw new ARQInternalErrorException("Literal does not start with a sign: " + lex);

        lex = lex.substring(1);
        return NodeFactory.createLiteral(lex, lang, dt);
    }

    protected Expr asExpr(Node n) {
        return ExprLib.nodeToExpr(n);
    }

    protected Node preConditionReifier(Node s, Node p, Path path, Node o, int line, int column) {
        if ( p != null )
            return p;
        if ( path instanceof P_Link )
            return ((P_Link)path).getNode();
        throwParseException("Only simple paths allowed with reifier syntax", line, column);
        return null;
    }

    // ---- IRIs with resolving
    // IRI(rel)

    protected Expr makeFunction_IRI(Expr expr) {
        return new E_IRI(profile.getBaseURI(), expr);
    }

    protected Expr makeFunction_URI(Expr expr) {
        return new E_URI(profile.getBaseURI(), expr);
    }

    // IRI(base, rel) or IRI(rel, null)
    protected Expr makeFunction_IRI(Expr expr1, Expr expr2) {
        if ( expr2 == null )
            return makeFunction_IRI(expr1);
        return new E_IRI2(expr1, profile.getBaseURI(), expr2);
    }

    protected Expr makeFunction_URI(Expr expr1, Expr expr2) {
        if ( expr2 == null )
            return makeFunction_URI(expr1);
        return new E_URI2(expr1, profile.getBaseURI(), expr2);
    }

   // Create a E_BNode function.
    protected Expr makeFunction_BNode() {
        return E_BNode.create();
    }

    protected Expr makeFunction_BNode(Expr expr) {
        return E_BNode.create(expr);
    }

    // ---- Literals
    // Strings, lang strings, dirlang strings, and datatyped literals.

    protected Node createLiteralString(String lexicalForm, int line, int column) {
        return NodeFactory.createLiteralString(lexicalForm);
    }

    protected Node createLiteralDT(String lexicalForm, String datatypeURI, int line, int column) {
        // Can't have type and lang tag in parsing.
        return createLiteralAny(lexicalForm, null, null, datatypeURI, line, column);
    }

    protected Node createLiteralLang(String lexicalForm, String langTagDir, int line, int column) {
        // Can't have type and lang tag in parsing.
        return createLiteralAny(lexicalForm, langTagDir, null, null, line, column);
    }

    /**
     * Create a literal, given all possible component parts.
     */
    private Node createLiteralAny(String lexicalForm, String langTag, String textDirStr, String datatypeURI, int line, int column) {
        Node n = null;
        // Can't have type and lang tag in parsing.
        if ( datatypeURI != null ) {
            if ( langTag != null || textDirStr != null )
                throw new ARQInternalErrorException("Datatype with lang/langDir");
            RDFDatatype dType = TypeMapper.getInstance().getSafeTypeByName(datatypeURI);
            n = NodeFactory.createLiteralDT(lexicalForm, dType);
            return n;
        }

        // datatypeURI is null
        if ( langTag == null && textDirStr == null )
            return NodeFactory.createLiteralString(lexicalForm);

         // Strip '@'
        langTag = langTag.substring(1);

        // See if we split langTag into language tag and base direction.
        String textDirStr2 = textDirStr;
        String langTag2 = langTag;
        if ( textDirStr == null ) {
            int idx = langTag.indexOf("--");
            if ( idx >= 0 ) {
                textDirStr2 = langTag.substring(idx+2);
                langTag2 = langTag.substring(0, idx);
            }
        }

        if ( langTag2 != null && textDirStr2 != null ) {
            if ( ! TextDirection.isValid(textDirStr2) )
                throw new QueryParseException("Illegal base direction: '"+textDirStr2+"'", line, column);
            return NodeFactory.createLiteralDirLang(lexicalForm, langTag2, textDirStr2);
        }
        // langTag != null, textDirStr == null.
        return NodeFactory.createLiteralLang(lexicalForm, langTag2);
    }
}
