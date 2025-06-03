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
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;

public class ShaclRulesParserBase extends LangParserBase {

    private List<ElementRule> rules = new ArrayList<>();
    private List<Triple> data = new ArrayList<>();

    public List<ElementRule> getRules() { return rules; }
    public List<Triple> getData() { return data; }

    // The finish "start, end" are position of the start of the syntax element

    protected void startRules() { state = BuildState.OUTER; }
    protected void finishRules() {
        if ( state != BuildState.OUTER )
            throwInternalStateException("finishRuleSet: Unfinished rule?");
        state = BuildState.NONE;
    }

    protected void startRule(int line, int column) {
        state = BuildState.RULE;
    }

    protected void finishRule(int line, int column) {
        if ( head == null )
            throwInternalStateException("Null head");
        if ( body == null )
            throwInternalStateException("Null body");
        ElementRule rule = new ElementRule(head, body);
        rules.add(rule);
        head = null;
        body = null;
        // Not data.
        state = BuildState.OUTER;
    }

    private static final boolean DEBUG = false;

    private static void debug(String fmt, int line, int column, Object...args) {
        if ( DEBUG ) {
            String posn = String.format("[%2d, %2d] ", line, column);
            System.out.print(posn);
            String msg = String.format(fmt, args);
            System.out.println(msg);
        }
    }

    private BasicPattern head = null;
    private ElementGroup body = null;

    protected void startHead(int line, int column) {
        debug("startHead", line, column);
        if ( head != null )
            throwInternalStateException("startHead: Already have a rule head");
        head = new BasicPattern();
        state = BuildState.HEAD;
    }

    protected void finishHead(int line, int column) {
        debug("finishHead", line, column);
    }

    protected void startBody(int line, int column) {
        debug("startBody", line, column);
        state = BuildState.BODY;
        body = new ElementGroup();
    }

    protected void finishBody(int line, int column) {
        debug("finishBody", line, column);
        state = BuildState.RULE;
    }

    // Allows paths and variables (unless paths expanded in the parser)
    protected void startTriplesBlock(int line, int column) {
        debug("startTriplesBlock", line, column);
        if ( elTriples != null )
            throwInternalStateException("Already gathering triples");
        // Start gather.
        elTriples = new ElementTriplesBlock();
    }

    protected void finishTriplesBlock(int line, int column) {
        debug("finishTriplesBlock", line, column);
        if ( elTriples.isEmpty() ) {}
        body.addElement(elTriples);
        elTriples = null;
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

    // -- Rule building state
    enum BuildState { NONE, OUTER, DATA, RULE, HEAD, BODY };

    BuildState state = BuildState.OUTER;

    private ElementTriplesBlock elTriples = null;

    // -- Accumulators

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

    protected void throwParseException(String msg, int line, int column) {
        throw new ShaclRulesParseException(msg, line, column);
    }

    protected void throwInternalStateException(String msg) {
        throw new IllegalStateException(msg);
    }

    protected void emitTriple(Node s, Node p, Node o, int line, int column) {
        debug("emitTriple", line, column);
        //super.emitTriple(line, column, s, p, o);
        accTriple(s, p, o, line, column);
    }

    protected void emitExpr(Expr expr, int line, int column) {
        debug("emitExpr", line, column);
        //System.out.println(Fmt.fmtSPARQL(expr, profile.getPrefixMap()));
        accExpr(expr);
    }

    private void accTriple(Node s, Node p , Node o, int line, int column) {
        Triple triple = Triple.create(s, p, o);

        switch(state) {
            case HEAD -> { head.add(triple); }
            case BODY -> { elTriples.addTriple(triple); }
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
                throwInternalStateException("Triple emited in state "+state);
            }
        }
    }

    private void accExpr(Expr expr) {
        ElementFilter filter = new ElementFilter(expr);
        body.addElement(filter);
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

    protected Node emitTripleReifier(Node reifierId, Node s, Node p, Node o, int line, int column) {
        return super.emitTripleReifier(line, column, reifierId, s, p, o);
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
    // Strings, lang strings, dirlang strings and datatyped literals.

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
