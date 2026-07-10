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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.TextDirection;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.lang.LangParserBase;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarAlloc;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RulesException;
import org.seaborne.jena.shacl_rules.ShaclRulesParser;
import org.seaborne.jena.shacl_rules.lang.RuleBodyElement;
import org.seaborne.jena.shacl_rules.lang.RuleHeadElement;
import org.seaborne.jena.shacl_rules.tuples.Tuple;
import org.slf4j.Logger;

public class RulesParserBase extends LangParserBase {

    private List<Rule> rules = new ArrayList<>();
    private List<Triple> data = new ArrayList<>();
    private List<Tuple> tupleData = new ArrayList<>();

    private LinkedHashSet<String> imports = new LinkedHashSet<>();

    private LinkedHashSet<String> transitiveProperties = new LinkedHashSet<>();
    private LinkedHashSet<String> symmetricProperties = new LinkedHashSet<>();
    private LinkedHashSet<Pair<String, String>> inverseProperties = new LinkedHashSet<>();

    public List<Rule> getRules() { return rules; }
    public List<Triple> getData() { return data; }
    public List<Tuple> getTupleData() { return tupleData; }

    public Set<String> getImports() { return imports; }
    public Set<String> getTransitiveProperties() { return transitiveProperties; }
    public Set<String> getSymmetricProperties() { return symmetricProperties; }
    public Set<Pair<String, String>> getInverseProperties() { return inverseProperties; }

    private boolean seenBaseIRI = false;
    public String getBaseIRI() {
        if ( seenBaseIRI )
            return super.profile.getBaseURI();
        return null;
    }

    @Override
    protected void setBase(String baseStr, int line, int column) {
        // Remember the parser has seen BASE.
        seenBaseIRI = true;
        // This will resolve the base.
        super.setBase(baseStr, line, column);
    }

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

    // INNER is the restricted body pattern for NOT.
    enum BuildState { NONE, OUTER, DATA, TUPLE_DATA, RULE, HEAD, BODY, INNER };
    protected BuildState state = BuildState.OUTER;

    // URI for the current rule.
    private String ruleURI = null;

    // These 2 accumulators are allocated then handed over to the AST object.
    private List<RuleHeadElement> headAcc = null;
    // Used while parsing the body.
    private List<RuleBodyElement> bodyAcc = null;
    // Used while parsing a negation or aggregation inner body.
    private List<RuleBodyElement> innerBodyAcc = null;

    // Used to accumulate nodes for a tuple.
    private final List<Node> tupleArgs = new ArrayList<>();

    // The whole body is "WHERE DATA"
    private boolean isGroundedRule;

    // The current rule element is NOT DATA
    private boolean isGroundedNegation;


    // Internal. The rule builder will calculate these.
    // We could give them to the builder if there is a way to also have them not set (API built, RDF Graph).
    private boolean hasNegation;
    private boolean hasAssignment;
    private boolean hasAggregation;

    private void clearState() {
        headAcc = null;
        bodyAcc = null;
        hasNegation = false;
        hasAssignment = false;
        hasAggregation = false;
        isGroundedRule = false;
        isGroundedNegation = false;
    }

    // ----

    protected void startRules() {
        state = BuildState.OUTER;
    }

    protected void finishRules() {
        Logger log = ShaclRulesParser.parserLogger;

        if ( state != BuildState.OUTER )
            throwInternalStateException("finishRuleSet: Unfinished rule?");
        state = BuildState.NONE;

        if ( !transitiveProperties.isEmpty()) {
            FmtLog.warn(log, "TRANSITIVE properties declared : %d", transitiveProperties.size());
        }
        if ( !symmetricProperties.isEmpty()) {
            FmtLog.warn(log, "SYMMETRIC properties declared : %d", symmetricProperties.size());
        }
        if ( !inverseProperties.isEmpty()) {
            FmtLog.warn(log, "INVERSE properties declared : %d", inverseProperties.size());
        }
    }

    protected void startRule(String uri, int line, int column) {
        if ( state != BuildState.OUTER )
            throwInternalStateException("startHead: Already in a rule");
        if ( bodyAcc != null )
            throwInternalStateException("startHead: Already in a rule");
        if ( headAcc != null )
            throwInternalStateException("startHead: Already in a rule");

        ruleURI = uri;
        headAcc = new ArrayList<>();
        bodyAcc = new ArrayList<>();
        state = BuildState.RULE;
    }

    protected void setGroundedRule(int line, int column) {
        this.isGroundedRule = true;
    }

    protected void finishRule(int line, int column) {
        if ( headAcc == null )
            throwInternalStateException("Null head");
        if ( bodyAcc == null )
            throwInternalStateException("Null body");

        Rule rule = Rule.newBuilder()
                .ruleIdentifier(ruleURI)
                .addBodyElements(bodyAcc)
                .addHeadElements(headAcc)
                .groundedRule(isGroundedRule)
                .build();
        rules.add(rule);
        clearState();
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

    // ---- Tuples

    protected void startTuple(int line, int column) {
        debug("startTuple", line, column);
    }
    protected void tupleArg(Node n) { tupleArgs.add(n); }


    protected void finishTuple(int line, int column) {
        debug("finishTuple", line, column);
        Tuple tuple = Tuple.create(tupleArgs);
        emitTuple(tuple, line, column);
        tupleArgs.clear();
    }

    // ---- Tuples

    // No variables, no paths
    protected void startData(int line, int column) {
        debug("startData", line, column);
        state = BuildState.DATA;
    }

    protected void finishData(int line, int column) {
        debug("finishData", line, column);
        state = BuildState.OUTER;
    }

    protected void startTupleDataBlock(int line, int column) {
        debug("startTupleData", line, column);
        state = BuildState.TUPLE_DATA;
    }

    protected void finishTupleDataBlock(int line, int column) {
        debug("finishTupleData", line, column);
        state = BuildState.OUTER;
    }

    protected void startDataTuple(int line, int column) {
        debug("startDataTuple", line, column);
        if ( state != BuildState.TUPLE_DATA )
            throwInternalStateException("Data tuples when in state "+state);
    }

    protected void finishDataTuple(int line, int column) {
        debug("finishDataTuple", line, column);
        Tuple tuple = Tuple.create(tupleArgs);
        emitDataTuple(tuple, line, column);
        tupleArgs.clear();
    }

    protected void startNegation(int line, int column) {
        debug("startNegation", line, column);
        state = BuildState.INNER;
        hasNegation = true;
        innerBodyAcc = new ArrayList<>();
    }

    protected void setGroundedNegation(int line, int column) {
        // Cleared in addRuleElement(List<RuleBodyElement> inner)
        this.isGroundedNegation = true;
    }

    protected void finishNegation(int line, int column) {
        debug("finishNegation", line, column);
        state = BuildState.BODY;
    }

    private void addHeadEltTriple(Triple tripleTemplate) {
        requireNonNull(tripleTemplate);
        headAcc.add(new RuleHeadElement.EltTripleTemplate(tripleTemplate));
    }

    private void addHeadEltTuple(Tuple tuple) {
        requireNonNull(tuple);
        headAcc.add(new RuleHeadElement.EltTupleTemplate(tuple));
    }

    private void addToBody(RuleBodyElement ruleElt) {
        requireNonNull(ruleElt);
        switch(state) {
            case BODY   -> bodyAcc.add(ruleElt);
            case INNER  -> innerBodyAcc.add(ruleElt);
            default     ->
                throwInternalStateException("Rule element emitted when in state "+state);
        }
    }

    // Triple pattern.
    private void addBodyEltTiple(Triple triplePattern) {
        requireNonNull(triplePattern);
        addToBody(new RuleBodyElement.EltTriplePattern(triplePattern));
    }

    // Tuple pattern.
    private void addBodyEltTuple(Tuple tuplePattern) {
        requireNonNull(tuplePattern);
        addToBody(new RuleBodyElement.EltTuplePattern(tuplePattern));
    }

    // Condition
    private void addBodyEltCondition(Expr expression) {
        requireNonNull(expression);
        addToBody(new RuleBodyElement.EltFilter(expression));
    }

    // Negation
    private void addBodyEltNegation(List<RuleBodyElement> inner) {
        requireNonNull(inner);
        RuleBodyElement rElt = new RuleBodyElement.EltNegation(inner, isGroundedNegation);
        addToBody(rElt);
        isGroundedNegation = false;
    }

    // Assignment
    private void addBodyEltAssignment(Var var, Expr expression) {
        requireNonNull(var);
        requireNonNull(expression);
        addToBody(new RuleBodyElement.EltAssignment(var, expression));
        hasAssignment = true;
    }

    private VarAlloc varAlloc = new VarAlloc(ARQConstants.allocPathVariables);

    protected void emitTriple(Node s, Node p, Path path, Node o, int line, int column) {
        debug("emitTriple", line, column);
        if ( p != null && path != null ) {
            String str = ParserLib.formatMessage("emitTriple : Both property and path are set", line, column);
            throw new InternalErrorException(str);
        }

        if ( p != null ) {
            accTriple(s, p, o, line, column);
            return;
        }
        PathExpand.pathExpand(varAlloc, s, path, o, (_s,_p,_o)->accTriple(_s, _p, _o, line, column));
    }

    @Override
    protected void emitTriple(Node s, Node p, Node o, int line, int column) {
        debug("emitDataTriple", line, column);
        accTriple(s, p, o, line, column);
    }

    protected void emitTuple(Tuple tuple, int line, int column) {
        debug("emitTuple", line, column);
        switch(state) {
            case HEAD -> { addHeadEltTuple(tuple); }
            case BODY -> { addBodyEltTuple(tuple); }
            case INNER -> {
                innerBodyAcc.addLast(new RuleBodyElement.EltTuplePattern(tuple));
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + state);
        }
    }

    protected void emitDataTuple(Tuple tuple, int line, int column) {
        debug("emitDataTuple", line, column);
        accDataTuple(tuple, line, column);
    }

    protected void emitNegation(int line, int column) {
        addBodyEltNegation(innerBodyAcc);
        innerBodyAcc = null;
    }

    protected void emitFilterExpr(Expr expr, int line, int column) {
        debug("emitFilterExpr", line, column);
        addBodyEltCondition(expr);
    }

    protected void emitAssignment(Var var, Expr expr, int line, int column) {
        debug("emitAssignment", line, column);
        addBodyEltAssignment(var, expr);
    }

    // << x y z >>
    @Override
    protected Node emitTripleReifier(Node reifierId, Node s, Node p, Node o, int line, int column) {
        //Node tripleTerm = NodeFactory.createTripleTerm(s, p, o);
        Node tripleTerm = createTripleTerm(s, p, o, line, column);
        if ( reifierId == null )
            reifierId = createBNode(line, column);
        accTriple(s, p, o, line, column);
        accTriple(reifierId, RDF.Nodes.reifies, tripleTerm, line, column);
        return reifierId;
    }

    @Override
    protected Node createTripleTerm(Node s, Node p, Node o, int line, int column) {
        // Allow variables.
        Node tripleTerm = NodeFactory.createTripleTerm(s, p, o);
        return tripleTerm;
    }

    protected void transitiveProperty(String iriStr) {
        // Add once.
        addOnce(transitiveProperties, iriStr);
    }

    protected void symmetricProperty(String iriStr) {
        addOnce(symmetricProperties, iriStr);
    }

    protected void inverseProperties(String iriStr1, String iriStr2) {
        addOnce(inverseProperties, Pair.create(iriStr1,  iriStr2));
    }

    private <X> void addOnce(LinkedHashSet<X> holder, X item) {
        holder.add(item);
    }

    protected void declareImport(String iri) {
        imports.add(iri);
    }

    // --

    protected RulesException createParseException(String msg, int line, int column) {
        return new ShaclRulesParseException(msg, line, column);
    }

    protected void throwInternalStateException(String msg) {
        throw new IllegalStateException(msg);
    }

    private void accTriple(Node s, Node p , Node o, int line, int column) {
        Triple triple = Triple.create(s, p, o);
        switch(state) {
            case HEAD -> { addHeadEltTriple(triple); }
            case BODY -> { addBodyEltTiple(Triple.create(s,p,o)); }
            case INNER -> {
                Triple triplePattern = Triple.create(s,p,o);
                innerBodyAcc.addLast(new RuleBodyElement.EltTriplePattern(triplePattern));
            }
            case DATA -> {
                if ( ! triple.isConcrete() )
                    throw createParseException("Triple must be concrete (no variables): "+triple, line, column);
                data.add(triple);
            }
            default -> {
                throwInternalStateException("Triple emitted in state "+state);
            }
        }
    }

    private void accDataTuple(Tuple tuple, int line, int column) {
        tupleData.add(tuple);
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
        throw createParseException("Only simple paths allowed with reifier syntax", line, column);
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
                throw createParseException("Illegal base direction: '"+textDirStr2+"'", line, column);
            return NodeFactory.createLiteralDirLang(lexicalForm, langTag2, textDirStr2);
        }
        // langTag != null, textDirStr == null.
        return NodeFactory.createLiteralLang(lexicalForm, langTag2);
    }
}
