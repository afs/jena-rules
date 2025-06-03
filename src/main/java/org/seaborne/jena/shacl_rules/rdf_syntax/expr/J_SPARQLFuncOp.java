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

package org.seaborne.jena.shacl_rules.rdf_syntax.expr;

import java.util.List;

import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.lib.DateTimeUtils;
import org.apache.jena.atlas.lib.RandomLib;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.expr.E_Regex;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.RegexEngine;
import org.apache.jena.sparql.expr.nodevalue.*;
import org.apache.jena.sparql.function.library.triple.TripleTermOps;

/**
 * SPARQL Functions and Operators, including extensions.
 * <p>
 * This class does not cover functional forms e.g. IF, COALESCE and BOUND.
 * <p>
 * Some function-like use of keywords in SPARQL do not make sense as standalone functions.
 * Some functions depend on the execution context and do not make sense outside of such a context
 * e.g. {@code BNODE(string)}, {@code NOW()}.
 */
public class J_SPARQLFuncOp {

    // ----

    private static boolean strict() {
        return  ARQ.isStrictMode();
    }

    // ---- All the implementations

    public static NodeValue sparql_plus(NodeValue nv1, NodeValue nv2) {
        if ( strict() )
            return XSDFuncOp.numAdd(nv1, nv2);
        return NodeValueOps.additionNV(nv1, nv2);
    }

    public static NodeValue sparql_subtract(NodeValue nv1, NodeValue nv2) {
        if ( strict() )
            return XSDFuncOp.numSubtract(nv1, nv2);
        return NodeValueOps.subtractionNV(nv1, nv2);
    }

    public static NodeValue sparql_multiply(NodeValue nv1, NodeValue nv2) {
        if ( strict() )
            return XSDFuncOp.numMultiply(nv1, nv2);

        return NodeValueOps.multiplicationNV(nv1, nv2);
    }

    public static NodeValue sparql_divide(NodeValue nv1, NodeValue nv2) {
        if ( strict() )
            return XSDFuncOp.numDivide(nv1, nv2);
        return NodeValueOps.divisionNV(nv1, nv2);
    }

    public static NodeValue sparql_unary_minus(NodeValue nv) {
        return XSDFuncOp.unaryMinus(nv);    }

    public static NodeValue sparql_unary_plus(NodeValue nv) {
       return XSDFuncOp.unaryPlus(nv);
    }

    public static NodeValue sparql_equals(NodeValue nv1, NodeValue nv2) {
        boolean b = NodeValue.sameValueAs(nv1, nv2);
        return NodeValue.booleanReturn(b);
    }

    public static NodeValue sparql_not_equals(NodeValue nv1, NodeValue nv2) {
        boolean b = NodeValue.notSameValueAs(nv1, nv2);
        return NodeValue.booleanReturn(b);
    }

    public static NodeValue sparql_greaterThan(NodeValue nv1, NodeValue nv2) {
        int r = NodeValue.compare(nv1, nv2);
        return NodeValue.booleanReturn(r == Expr.CMP_GREATER);
    }

    public static NodeValue sparql_lessThan(NodeValue nv1, NodeValue nv2) {
        int r = NodeValue.compare(nv1, nv2);
        return NodeValue.booleanReturn(r == Expr.CMP_LESS);
    }

    public static NodeValue sparql_greaterThanOrEqual(NodeValue nv1, NodeValue nv2) {
        int r = NodeValue.compare(nv1, nv2);
        return NodeValue.booleanReturn(r == Expr.CMP_GREATER || r == Expr.CMP_EQUAL);
    }

    public static NodeValue sparql_lessThanOrEqual(NodeValue nv1, NodeValue nv2) {
        int r = NodeValue.compare(nv1, nv2);
        return NodeValue.booleanReturn(r == Expr.CMP_LESS || r == Expr.CMP_EQUAL);
    }

    // and,or,not as functions (arguments have been evaluated)

    public static NodeValue arq_function_and(NodeValue nv1, NodeValue nv2) {
        boolean arg1 = XSDFuncOp.effectiveBooleanValue(nv1);
        boolean arg2 = XSDFuncOp.effectiveBooleanValue(nv2);
        return NodeValue.booleanReturn(arg1 && arg2);
    }

    public static NodeValue arq_function_or(NodeValue nv1, NodeValue nv2) {
        boolean arg1 = XSDFuncOp.effectiveBooleanValue(nv1);
        boolean arg2 = XSDFuncOp.effectiveBooleanValue(nv2);
        return NodeValue.booleanReturn(arg1 || arg2);
    }

  public static NodeValue arq_function_not(NodeValue nv) {
      boolean arg = XSDFuncOp.effectiveBooleanValue(nv);
      return NodeValue.booleanReturn(!arg);
  }

  public static NodeValue arq_version() { return ARQFuncOp.version(); }

  // Functional forms (not functions)
  // See J_FunctionalForm

//    sparql_operator_and
//    sparql_operator_or
//    sparql_operator_not -- strictly a function but convenient to group with other logical_*

//    sparql_bound
//    sparql_if
//    sparql_coalesce

//    sparql_filter_exists
//    sparql_filter_not_exists

//    sparql_in
//    sparql_not_in


    public static NodeValue sparql_sameTerm(NodeValue nv1, NodeValue nv2) { return NodeFunctions.sameTerm(nv1, nv2); }
    public static NodeValue sparql_sameValue(NodeValue nv1, NodeValue nv2) {
        // Need to deal with NaN.
        if ( ( nv1.isDouble()||nv1.isFloat() ) && ( nv2.isDouble()||nv2.isFloat() ) ) {
            double d1 = nv1.getDouble();
            double d2 = nv2.getDouble();
            if ( Double.isNaN(d1) &&Double.isNaN(d2) )
                return NodeValue.TRUE;
        }
        boolean b = NodeValue.sameValueAs(nv1, nv2);
        return NodeValue.booleanReturn(b);
    }

    // Old name:: public static NodeValue sparql_RDFterm_equal(NodeValue nv1, NodeValue nv2) { return null; }
    public static NodeValue sparql_isIRI(NodeValue nv)      { return NodeFunctions.isIRI(nv); }
    public static NodeValue sparql_isURI(NodeValue nv)      { return NodeFunctions.isURI(nv); }
    public static NodeValue sparql_isBlank(NodeValue nv)    { return NodeFunctions.isBlank(nv); }
    public static NodeValue sparql_isLiteral(NodeValue nv)  { return NodeFunctions.isLiteral(nv); }
    public static NodeValue sparql_isNumeric(NodeValue nv)  { return NodeFunctions.isNumeric(nv); }
    public static NodeValue sparql_str(NodeValue nv)        { return NodeFunctions.str(nv); }

    public static NodeValue sparql_lang(NodeValue nv)       { return NodeFunctions.str(nv); }
    public static NodeValue sparql_langdir(NodeValue nv)    { return NodeFunctions.langdir(nv); }
    public static NodeValue sparql_haslang(NodeValue nv)    { return NodeFunctions.hasLang(nv); }
    public static NodeValue sparql_haslangdir(NodeValue nv) { return NodeFunctions.hasLangDir(nv); }
    public static NodeValue sparql_datatype(NodeValue nv)   { return NodeFunctions.datatype(nv); }

    // Term functions : NodeFunctions

    public static NodeValue sparql_iri(NodeValue nv) { return NodeFunctions.iri(nv, null); }
    public static NodeValue sparql_uri(NodeValue nv) { return sparql_iri(nv); }

    // Extension. Two argument form of IRI() and URI()
    public static NodeValue arq_iri(NodeValue nv, NodeValue nvBase) { return NodeFunctions.iri(nv, nvBase.getString()); }
    public static NodeValue arq_uri(NodeValue nv, NodeValue nvBase) { return NodeFunctions.iri(nv, nvBase.getString()); }

    // Only BNODE(), not BNODE(str)
    public static NodeValue sparql_bnode() { return null; }

    // Not a function - depends on "current row".
    //public static NodeValue sparql_bnode(NodeValue nv) { return null; }

    public static NodeValue sparql_strdt(NodeValue nv1, NodeValue nv2) { return NodeFunctions.strDatatype(nv1, nv2); }
    public static NodeValue sparql_strlang(NodeValue nv1, NodeValue nv2) { return NodeFunctions.strLang(nv1, nv2); }
    public static NodeValue sparql_strlangdir(NodeValue nv1, NodeValue nv2, NodeValue nv3) { return NodeFunctions.strLangDir(nv1, nv2, nv3); }

    public static NodeValue sparql_uuid() { return NodeFunctions.uuid(); }
    public static NodeValue sparql_struuid() { return NodeFunctions.struuid(); }

    public static NodeValue sparql_strlen(NodeValue nv) { return XSDFuncOp.strlen(nv); }
    public static NodeValue sparql_substr(NodeValue nv1, NodeValue nv2) { return XSDFuncOp.substring(nv1, nv2) ; }
    public static NodeValue sparql_substr(NodeValue nv1, NodeValue nv2, NodeValue nv3) { return XSDFuncOp.substring(nv1, nv2, nv3) ; }

    public static NodeValue sparql_ucase(NodeValue nv) { return XSDFuncOp.strUpperCase(nv); }
    public static NodeValue sparql_lcase(NodeValue nv) { return XSDFuncOp.strLowerCase(nv); }
    public static NodeValue sparql_strstarts(NodeValue nv1, NodeValue nv2) { return XSDFuncOp.strStartsWith(nv1, nv2); }
    public static NodeValue sparql_strends(NodeValue nv1, NodeValue nv2) { return XSDFuncOp.strEndsWith(nv1, nv2); }

    public static NodeValue sparql_contains(NodeValue nv1, NodeValue nv2) { return XSDFuncOp.strContains(nv1, nv2); }
    public static NodeValue sparql_strbefore(NodeValue nv1, NodeValue nv2) { return XSDFuncOp.strBefore(nv1, nv2); }
    public static NodeValue sparql_strafter(NodeValue nv1, NodeValue nv2) { return XSDFuncOp.strAfter(nv1, nv2); }

    public static NodeValue sparql_concat(NodeValue...args) {
        return XSDFuncOp.strConcat(List.of(args));
    }

    public static NodeValue sparql_langMatches(NodeValue nv1, NodeValue nv2) { return NodeFunctions.langMatches(nv1, nv2); }

    private static record CacheKey(String pattern, String flags) {}
    private static Cache<CacheKey, RegexEngine> regexEngineCache = CacheFactory.createCache(20);
    private static RegexEngine getRegexEngine(String patternStr, String flagsStr) {
        CacheKey cacheKey = new CacheKey(patternStr, flagsStr);
        return regexEngineCache.get(cacheKey, (key)->E_Regex.makeRegexEngine(key.pattern, key.flags));
    }

    // Arity 2/3
    public static NodeValue sparql_regex(NodeValue string, NodeValue pattern) { return sparql_regex(string, pattern, null); }
    public static NodeValue sparql_regex(NodeValue string, NodeValue pattern, NodeValue flags) {
        // Cache - this means regexes are compiled once.
        String patternStr = pattern.getString();
        String flagsStr = flags.getString();
        RegexEngine regexEngine = getRegexEngine(patternStr, flagsStr);
        String str = string.getString();
        boolean b = regexEngine.match(str);
        return NodeValue.booleanReturn(b);
    }

    // Arity 3/4
    public static NodeValue sparql_replace(NodeValue nvStr, NodeValue nvPattern, NodeValue nvReplacement)
    { return XSDFuncOp.strReplace(nvStr, nvPattern, nvReplacement); }

    public static NodeValue sparql_replace(NodeValue nvStr, NodeValue nvPattern, NodeValue nvReplacement, NodeValue envFlags)
    { return XSDFuncOp.strReplace(nvStr, nvPattern, nvReplacement, envFlags); }

    public static NodeValue sparql_encode(NodeValue nv) { return XSDFuncOp.strEncodeForURI(nv); }
    public static NodeValue sparql_abs(NodeValue nv)    { return XSDFuncOp.abs(nv); }
    public static NodeValue sparql_round(NodeValue nv)  { return XSDFuncOp.round(nv); }
    public static NodeValue sparql_ceil(NodeValue nv)   { return XSDFuncOp.ceiling(nv); }
    public static NodeValue sparql_floor(NodeValue nv)  { return XSDFuncOp.floor(nv); }

    public static NodeValue sparql_rand() {
        double d = RandomLib.random.nextDouble();
        return NodeValue.makeDouble(d);
    }

    // Warning : not scoped to evaluation.
    public static NodeValue sparql_now() { return NodeValue.makeDateTime(DateTimeUtils.nowAsXSDDateTimeString()); }

    public static NodeValue sparql_year(NodeValue nv)       { return XSDFuncOp.getYear(nv); }
    public static NodeValue sparql_month(NodeValue nv)      { return XSDFuncOp.getMonth(nv); }
    public static NodeValue sparql_day(NodeValue nv)        { return XSDFuncOp.getDay(nv); }
    public static NodeValue sparql_hours(NodeValue nv)      { return XSDFuncOp.getHours(nv); }
    public static NodeValue sparql_minutes(NodeValue nv)    { return XSDFuncOp.getMinutes(nv); }
    public static NodeValue sparql_seconds(NodeValue nv)    { return XSDFuncOp.getSeconds(nv); }
    // Returns duration
    public static NodeValue sparql_timezone(NodeValue nv)   { return XSDFuncOp.dtGetTimezone(nv); }
    // Returns string
    public static NodeValue sparql_tz(NodeValue nv)         { return XSDFuncOp.dtGetTZ(nv); }

    // ARQ ADJUST is all purpose - i.e. xsd:dateTime/xsd:date/xsd:time
    public static NodeValue arq_adjust(NodeValue nv)        { return XSDFuncOp.adjustToTimezone(nv, null); }
    public static NodeValue arq_adjust(NodeValue nv1, NodeValue nv2)        { return XSDFuncOp.adjustToTimezone(nv1, nv2); }


    public static NodeValue sparql_triple(NodeValue s, NodeValue p, NodeValue o)    { return TripleTermOps.fnTriple(s, p, o); }
    public static NodeValue sparql_subject(NodeValue tripleTerm)                    { return TripleTermOps.tripleSubject(tripleTerm); }
    public static NodeValue sparql_predicate(NodeValue tripleTerm)                  { return TripleTermOps.triplePredicate(tripleTerm); }
    public static NodeValue sparql_object(NodeValue tripleTerm)                     { return TripleTermOps.tripleObject(tripleTerm); }
    public static NodeValue sparql_isTriple(NodeValue nv)                           { return TripleTermOps.isTriple(nv); }

    public static NodeValue sparql_md5(NodeValue nv)    { return NodeValueDigest.calculateDigest(nv, "MD-5"); }
    public static NodeValue sparql_sha1(NodeValue nv)   { return NodeValueDigest.calculateDigest(nv, "SHA-1"); }
    public static NodeValue sparql_sha224(NodeValue nv) { return NodeValueDigest.calculateDigest(nv, "SHA-2224"); }
    public static NodeValue sparql_sha256(NodeValue nv) { return NodeValueDigest.calculateDigest(nv, "SHA-256"); }
    public static NodeValue sparql_sha384(NodeValue nv) { return NodeValueDigest.calculateDigest(nv, "SHA-384"); }
    public static NodeValue sparql_sha512(NodeValue nv) { return NodeValueDigest.calculateDigest(nv, "SHA-512"); }
}

