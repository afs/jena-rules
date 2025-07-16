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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.function.FunctionEnv;

class FunctionEverything {

    // FunctionEverything, J_FunctionalForm, J_SPARQLFuncOp replace SPARQLDispatch, SPARQLFuncOp

    // API:
    //    Execution of RDF expressions : NodeExpressions


    /**
     * Build the mappings
     */
    private static void initTables(Map<String, Call> mapDispatch, Map<String, CallFF> mapDispatchFF,
                                   Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI) {

        // Functional forms (not functions)

        entryFunctionForm2(mapDispatchFF, mapBuild, mapFunctionURI, "sparql:logical-and", E_LogicalAnd.class, "&&", E_LogicalAnd::new, J_FunctionalForms::sparql_logical_and);
        entryFunctionForm2(mapDispatchFF, mapBuild, mapFunctionURI, "sparql:logical-or", E_LogicalOr.class, "||", E_LogicalOr::new, J_FunctionalForms::sparql_logical_and);
        entryFunctionForm1(mapDispatchFF, mapBuild, mapFunctionURI, "sparql:logical-not", E_LogicalNot.class, "!", E_LogicalNot::new, J_FunctionalForms::sparql_logical_not);

        // As functions (extension)

        //entry(mapDispatch, mapBuild, "sparql:logical-and", E_LogicalAnd.class, "&&", E_LogicalAnd::new, SPARQLFuncOp::logical_and);
        //entry(mapDispatch, mapBuild, "sparql:logical-not", E_LogicalNot.class, "!", E_LogicalNot::new, SPARQLFuncOp::logical_not);
        //entry(mapDispatch, mapBuild, "sparql:logical-or", E_LogicalOr.class, "||", E_LogicalOr::new, SPARQLFuncOp::logical_or);

        // SHACL sh:if node expressions name argument format.
        entryFunctionForm3(mapDispatchFF, mapBuild, mapFunctionURI, "sh:if", null, "sh:if", null, J_FunctionalForms::shacl_if);

        entryFunctionFormN(mapDispatchFF, mapBuild, mapFunctionURI, "sparql:coalesce", E_Coalesce.class, "COALESCE", E_Coalesce::new, J_FunctionalForms::sparql_coalesce);

        // Adjust for the constructor which is E_OneOf(Expr, ExporList) and same of E_NotOneOf
        CreateN<Expr> makeOneOf = exprList -> {
            if ( exprList.size() < 1) {}
            Expr expr = exprList.get(0);
            ExprList exprList2 = exprList.tail(1);
            return new E_OneOf(expr, exprList2);
        };
        CreateN<Expr> makeNotOneOf = exprList -> {
            if ( exprList.size() < 1) {}
            Expr expr = exprList.get(0);
            ExprList exprList2 = exprList.tail(1);
            return new E_NotOneOf(expr, exprList2);
        };

        entryFunctionFormN(mapDispatchFF, mapBuild, mapFunctionURI, "sparql:in", E_OneOf.class, "IN", makeOneOf, J_FunctionalForms::sparql_in);
        entryFunctionFormN(mapDispatchFF, mapBuild, mapFunctionURI, "sparql:not_in", E_NotOneOf.class, "NOT IN", makeNotOneOf, J_FunctionalForms::sparql_not_in);
        entryFunctionForm1(mapDispatchFF, mapBuild, mapFunctionURI, "sparql:bound", E_Bound.class, "BOUND", E_Bound::new, J_FunctionalForms::sparql_bound);

        // URI function call.
        //entry(mapDispatch, mapBuild, mapFunctionURI, "arq:function", E_Function.class, "FUNCTION", E_Function::new, J_SPARQLFuncOp::function);

        // EXISTS, NOT EXISTS
        //entryFunctionFormOp(mapDispatchFF, mapDispatch, mapBuild, "sparql:filter-exists", E_Exists.class, "EXISTS", E_Exists::new, J_FunctionalForms::filter_exists);
        //entryFunctionFormOp(mapDispatchFF, mapDispatch, mapBuild, "sparql:filter-not-exists", E_NotExists.class, "NOT EXISTS", E_NotExists::new, J_FunctionalForms::filter_not_exists);

        // ---- Functions.

        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:iri", E_IRI.class, "IRI", E_IRI::new, J_SPARQLFuncOp::sparql_iri);

        // Operators
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:plus", E_Add.class, "+", E_Add::new, J_SPARQLFuncOp::sparql_plus);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:subtract", E_Subtract.class, "-", E_Subtract::new, J_SPARQLFuncOp::sparql_subtract);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:multiply", E_Multiply.class, "*", E_Multiply::new, J_SPARQLFuncOp::sparql_multiply);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:divide", E_Divide.class, "/", E_Divide::new, J_SPARQLFuncOp::sparql_divide);

        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:equals", E_Equals.class, "=", E_Equals::new, J_SPARQLFuncOp::sparql_equals);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:equals", E_NotEquals.class, "!=", E_Equals::new, J_SPARQLFuncOp::sparql_not_equals);

        //        // ARQ
        //        entry(mapDispatch, mapBuild, mapFunctionURI, "arq:idiv", E_OpNumericIntegerDivide.class, "IDIV", E_OpNumericIntegerDivide::new, J_SPARQLFuncOp::arq_idiv);
        //        // ARQ
        //        entry(mapDispatch, mapBuild, mapFunctionURI, "arq:mod", E_OpNumericMod.class, "MOD", E_OpNumericMod::new, J_SPARQLFuncOp::arq_mod);

        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:unary-minus", E_UnaryMinus.class, "-", E_UnaryMinus::new, J_SPARQLFuncOp::sparql_unary_minus);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:unary-plus", E_UnaryPlus.class, "+", E_UnaryPlus::new, J_SPARQLFuncOp::sparql_unary_plus);

        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:greater-than", E_GreaterThan.class, ">", E_GreaterThan::new, J_SPARQLFuncOp::sparql_greater_than);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:greater-than-or-equal", E_GreaterThanOrEqual.class, ">=", E_GreaterThanOrEqual::new, J_SPARQLFuncOp::sparql_greater_than_or_equal);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:less-than", E_LessThan.class, ">", E_LessThan::new, J_SPARQLFuncOp::sparql_less_than);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:less-than-or-equal", E_LessThanOrEqual.class, ">=", E_LessThanOrEqual::new, J_SPARQLFuncOp::sparql_less_than_or_equal);

        // Specials as functions
        //        // E_ not right
        //        entry1(mapDispatch, mapBuild, mapFunctionURI, "arq:function-not", E_LogicalNot.class, "!", E_LogicalNot::new, J_SPARQLFuncOp::arq_function_not);
        //        entry2(mapDispatch, mapBuild, mapFunctionURI, "arq:function-and", E_LogicalAnd.class, "&&", E_LogicalAnd::new, J_SPARQLFuncOp::arq_function_and);
        //        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:function-or", E_LogicalOr.class, "||", E_LogicalOr::new, J_SPARQLFuncOp::arq_function_or);

        entry0(mapDispatch, mapBuild, mapFunctionURI, "sparql:now", E_Now.class, "NOW", E_Now::new, J_SPARQLFuncOp::sparql_now);

        // RDF Term related

        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:hasLangdir", E_HasLangDir.class, "hasLANGDIR", E_HasLangDir::new, J_SPARQLFuncOp::sparql_hasLangdir);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:hasLang", E_HasLang.class, "hasLANG", E_HasLang::new, J_SPARQLFuncOp::sparql_hasLang);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:langdir", E_LangDir.class, "LANGDIR", E_LangDir::new, J_SPARQLFuncOp::sparql_langdir);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:lang", E_Lang.class, "LANG", E_Lang::new, J_SPARQLFuncOp::sparql_lang);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:langMatches", E_LangMatches.class, "LANGMATCHES", E_LangMatches::new, J_SPARQLFuncOp::sparql_langMatches);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:datatype", E_Datatype.class, "DATATYPE", E_Datatype::new, J_SPARQLFuncOp::sparql_datatype);

        entry3(mapDispatch, mapBuild, mapFunctionURI, "sparql:strlangdir", E_StrLangDir.class, "STRLANGDIR", E_StrLangDir::new, J_SPARQLFuncOp::sparql_strlangdir);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:strlang", E_StrLang.class, "STRLANG", E_StrLang::new, J_SPARQLFuncOp::sparql_strlang);

        //        entry(mapDispatch, mapBuild, mapFunctionURI, "arq:uri2", E_URI2.class, "URI2", E_URI2::new, J_SPARQLFuncOp::sparql_arq_uri2);

        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:iri", E_IRI.class, "IRI", E_IRI::new, J_SPARQLFuncOp::sparql_uri);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:uri", E_URI.class, "URI", E_URI::new, J_SPARQLFuncOp::sparql_uri);

        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:isBlank", E_IsBlank.class, "isBlank", E_IsBlank::new, J_SPARQLFuncOp::sparql_isBlank);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:isIRI", E_IsIRI.class, "isIRI", E_IsIRI::new, J_SPARQLFuncOp::sparql_isIRI);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:isURI", E_IsURI.class, "isURI", E_IsURI::new, J_SPARQLFuncOp::sparql_isURI);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:isLiteral", E_IsLiteral.class, "isLITERAL", E_IsLiteral::new, J_SPARQLFuncOp::sparql_isLiteral);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:isNumeric", E_IsNumeric.class, "isNUMERIC", E_IsNumeric::new, J_SPARQLFuncOp::sparql_isNumeric);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:isTriple", E_IsTriple.class, "isTRIPLE", E_IsTriple::new, J_SPARQLFuncOp::sparql_isTriple);

        entry2(mapDispatch, mapBuild, mapFunctionURI, "arq:iri2", E_URI2.class, "URI", E_IRI2::new, J_SPARQLFuncOp::arq_iri);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "arq:uri2", E_URI2.class, "URI", E_IRI2::new, J_SPARQLFuncOp::arq_iri);

        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:strdt", E_StrDatatype.class, "STRDT", E_StrDatatype::new, J_SPARQLFuncOp::sparql_strdt);

        entry0(mapDispatch, mapBuild, mapFunctionURI, "sparql:bnode", E_BNode.BNode0.class, "BNODE", E_BNode::create, J_SPARQLFuncOp::sparql_bnode);
        entry0(mapDispatch, mapBuild, mapFunctionURI, "sparql:bnode", E_BNode.BNode1.class, "BNODE", E_BNode::create, J_SPARQLFuncOp::sparql_bnode);

        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:sameTerm", E_SameTerm.class, "sameTERM", E_SameTerm::new, J_SPARQLFuncOp::sparql_sameTerm);
        entry2(mapDispatch, mapBuild, mapFunctionURI, "sparql:sameValue", E_SameValue.class, "sameVALUE", E_SameValue::new, J_SPARQLFuncOp::sparql_sameValue);

        // Datetime

        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:year", E_DateTimeYear.class, "YEAR", E_DateTimeYear::new, J_SPARQLFuncOp::sparql_year);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:month", E_DateTimeMonth.class, "MONTH", E_DateTimeMonth::new, J_SPARQLFuncOp::sparql_month);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:day", E_DateTimeDay.class, "DAY", E_DateTimeDay::new, J_SPARQLFuncOp::sparql_day);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:hours", E_DateTimeHours.class, "HOURS", E_DateTimeHours::new, J_SPARQLFuncOp::sparql_hours);
        entry1(mapDispatch, mapBuild, mapFunctionURI, "sparql:minutes", E_DateTimeMinutes.class, "MINUTES", E_DateTimeMinutes::new, J_SPARQLFuncOp::sparql_minutes);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:seconds", E_DateTimeSeconds.class, "SECONDS", E_DateTimeSeconds::new, J_SPARQLFuncOp::sparql_seconds);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:timezone", E_DateTimeTimezone.class, "TIMEZONE", E_DateTimeTimezone::new, J_SPARQLFuncOp::sparql_timezone);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:tz", E_DateTimeTZ.class, "TZ", E_DateTimeTZ::new, J_SPARQLFuncOp::sparql_tz);

        // ARQ
        entry12(mapDispatch, mapBuild, mapFunctionURI,"arq:adjust", E_AdjustToTimezone.class, "ADJUST",
                E_AdjustToTimezone::new, J_SPARQLFuncOp::arq_adjust,
                E_AdjustToTimezone::new, J_SPARQLFuncOp::arq_adjust);

        // Numerics

        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:abs", E_NumAbs.class, "ABS", E_NumAbs::new, J_SPARQLFuncOp::sparql_abs);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:ceil", E_NumCeiling.class, "CEIL", E_NumCeiling::new, J_SPARQLFuncOp::sparql_ceil);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:floor", E_NumFloor.class, "FLOOR", E_NumFloor::new, J_SPARQLFuncOp::sparql_floor);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:round", E_NumRound.class, "ROUND", E_NumRound::new, J_SPARQLFuncOp::sparql_round);
        entry0(mapDispatch, mapBuild, mapFunctionURI,"sparql:rand", E_Random.class, "RAND", E_Random::new, J_SPARQLFuncOp::sparql_rand);

        // Hash functions

        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:md5", E_MD5.class, "MD5", E_MD5::new, J_SPARQLFuncOp::sparql_md5);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha1", E_SHA1.class, "SHA1", E_SHA1::new, J_SPARQLFuncOp::sparql_sha1);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha224", E_SHA224.class, "SHA224", E_SHA224::new, J_SPARQLFuncOp::sparql_sha224);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha256", E_SHA256.class, "SHA256", E_SHA256::new, J_SPARQLFuncOp::sparql_sha256);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha384", E_SHA384.class, "SHA384", E_SHA384::new, J_SPARQLFuncOp::sparql_sha384);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha512", E_SHA512.class, "SHA512", E_SHA512::new, J_SPARQLFuncOp::sparql_sha512);

        // Strings

        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:str", E_Str.class, "STR", E_Str::new, J_SPARQLFuncOp::sparql_str);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:strlen", E_StrLength.class, "STRLEN", E_StrLength::new, J_SPARQLFuncOp::sparql_strlen);

        // Arity 2/3
        entry23(mapDispatch, mapBuild, mapFunctionURI,"sparql:regex", E_Regex.class, "REGEX",
                E_Regex::new, J_SPARQLFuncOp::sparql_regex,
                E_Regex::new, J_SPARQLFuncOp::sparql_regex);

        entry2(mapDispatch, mapBuild, mapFunctionURI,"sparql:strafter", E_StrAfter.class, "strAFTER", E_StrAfter::new, J_SPARQLFuncOp::sparql_strafter);
        entry2(mapDispatch, mapBuild, mapFunctionURI,"sparql:strbefore", E_StrBefore.class, "strBEFORE", E_StrBefore::new, J_SPARQLFuncOp::sparql_strbefore);
        entry2(mapDispatch, mapBuild, mapFunctionURI,"sparql:strstarts", E_StrStartsWith.class, "STRSTARTS", E_StrStartsWith::new, J_SPARQLFuncOp::sparql_strstarts);
        entry2(mapDispatch, mapBuild, mapFunctionURI,"sparql:strends", E_StrEndsWith.class, "STRENDS", E_StrEndsWith::new, J_SPARQLFuncOp::sparql_strends);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:lcase", E_StrLowerCase.class, "LCASE", E_StrLowerCase::new, J_SPARQLFuncOp::sparql_lcase);

        // Arity 3/4
        entry34(mapDispatch, mapBuild, mapFunctionURI,"sparql:replace", E_StrReplace.class, "REPLACE",
                E_StrReplace::new, J_SPARQLFuncOp::sparql_replace,
                E_StrReplace::new, J_SPARQLFuncOp::sparql_replace);

        entry3(mapDispatch, mapBuild, mapFunctionURI,"sparql:substr", E_StrSubstring.class, "SUBSTR", E_StrSubstring::new, J_SPARQLFuncOp::sparql_substr);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:ucase", E_StrUpperCase.class, "UCASE", E_StrUpperCase::new, J_SPARQLFuncOp::sparql_ucase);

        // Arity N
        entryN(mapDispatch, mapBuild, mapFunctionURI,"sparql:concat", E_StrConcat.class, "CONCAT", E_StrConcat::new, J_SPARQLFuncOp::sparql_concat);
        entry2(mapDispatch, mapBuild, mapFunctionURI,"sparql:contains", E_StrContains.class, "CONTAINS", E_StrContains::new, J_SPARQLFuncOp::sparql_contains);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:encode-for-uri", E_StrEncodeForURI.class, "ENCODE_FOR_URI", E_StrEncodeForURI::new, J_SPARQLFuncOp::sparql_encode_for_uri);

        // UUIDs

        entry0(mapDispatch, mapBuild, mapFunctionURI,"sparql:uuid", E_UUID.class, "UUID", E_UUID::new, J_SPARQLFuncOp::sparql_uuid);
        entry0(mapDispatch, mapBuild, mapFunctionURI,"sparql:struuid", E_StrUUID.class, "STRUUID", E_StrUUID::new, J_SPARQLFuncOp::sparql_struuid);

        // Triple terms

        entry3(mapDispatch, mapBuild, mapFunctionURI,"sparql:triple", E_TripleFn.class, "TRIPLE", E_TripleFn::new, J_SPARQLFuncOp::sparql_triple);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:object", E_TripleObject.class, "OBJECT", E_TripleObject::new, J_SPARQLFuncOp::sparql_object);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:predicate", E_TriplePredicate.class, "PREDICATE", E_TriplePredicate::new, J_SPARQLFuncOp::sparql_predicate);
        entry1(mapDispatch, mapBuild, mapFunctionURI,"sparql:subject", E_TripleSubject.class, "SUBJECT", E_TripleSubject::new, J_SPARQLFuncOp::sparql_subject);

        // ARQ

        //        entry(mapDispatch, mapBuild, mapFunctionURI,"arq:call", E_Call.class, "CALL", E_Call::new, J_SPARQLFuncOp::);
        //        entry(mapDispatch, mapBuild, mapFunctionURI,"arq:cast", E_Cast.class, "CAST", E_Cast::new, J_SPARQLFuncOp::);
        entry0(mapDispatch, mapBuild, mapFunctionURI,"arq:version", E_Version.class, "VERISON", E_Version::new, J_SPARQLFuncOp::arq_version);
    }

    /**
     * Look up an URI to get a callable object.
     */
    static Call getCall(String uri) {
        return mapDispatch().get(uri);
    }

    /**
     * Look up an URI to get a callable functional form object.
     */
    static CallFF getCallFF(String uri) {
        return mapDispatchFF().get(uri);
    }

    /**
     * Look up an URI to get a function that will build an {@Expr}.
     */
    static Build getBuild(String uri) {
        return mapBuild().get(uri);
    }

    /**
     * Look up a {@Expr} to get its URI.
     * This could be a methods on Expr.
     * For now, while development is in progress, there is a separate table based on class.
     */
    static String getUriForExpr(Expr expr) {
        return mapFunctionURI().get(expr.getClass());
    }

    static class LazyInit {
        // Function URI to callable
        // Used to load the ARQ function registry
        private static Map<String, Call> mapDispatch = new HashMap<>();

        // Function URI to callable for functional forms.
        private static Map<String, CallFF> mapDispatchFF = new HashMap<>();

        // Function URI to build function ; Node expression to Expr
        static Map<String, Build> mapBuild = new HashMap<>();

        // Class to URI,
        static Map<Class<?>, String> mapFunctionURI = new HashMap<>();

        // Lazy initialization
        static {
            FunctionEverything.initTables(mapDispatch, mapDispatchFF, mapBuild, mapFunctionURI);
        }
    }

    static Map<String, Call> mapDispatch() { return LazyInit.mapDispatch; }
    static Map<String, CallFF> mapDispatchFF() { return LazyInit.mapDispatchFF; }
    static Map<String, Build> mapBuild() { return LazyInit.mapBuild; }
    static Map<Class<?>, String> mapFunctionURI() { return LazyInit.mapFunctionURI; }

    // ----

    //@formatter:off
    // The table uses prefixes names for URIs for readability.
    private static PrefixMap prefixMap = PrefixMapFactory.create(Map.of
                                                                 ("rdf",     "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                                                                  "sh",      "http://www.w3.org/ns/shacl#",
                                                                  "sparql",  "http://www.w3.org/ns/sparql#",
                                                                  "arq",     "http://jena.apache.org/ARQ/function#"));
    //@formatter:on

    private static String expandName(String x) {
        String z = prefixMap.expand(x);
        return (z != null) ? z : x;
    }

    // ---- Registration

    // Arity 0
    private static <X> void entry0(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                   String uriName, Class<? extends Expr> implClass, String sparqlName,
                                   Create0<? extends Expr> maker, Function0 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 0 )
                throw new ShaclException("Wrong number of arguments expressions: expected 0, got "+exprs.length);
            return maker.create();
        };
        Call call = args->{
            if ( args.length != 1 ) throw exception("%s: Expected zero arguments. Got %d", uri, args.length);
            return function.exec();
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // Arity 1
    private static <X> void entry1(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                   String uriName, Class<? extends Expr> implClass, String sparqlName,
                                   Create1<? extends Expr> maker, Function1 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 1 )
                throw new ShaclException("Wrong number of arguments expressions: expected 1, got "+exprs.length);
            return maker.create(exprs[0]);
        };
        Call call = args->{
            if ( args.length != 1 ) throw exception("%s: Expected one arguments. Got %d", uri, args.length);
            return function.exec(args[0]);
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // Arity 1/2
    private static <X> void entry12(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                    String uriName, Class<? extends Expr> implClass, String sparqlName,
                                    Create1<? extends Expr> maker1, Function1 function1,
                                    Create2<? extends Expr> maker2, Function2 function2) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length == 1 )
                return maker1.create(exprs[0]);
            if ( exprs.length == 2 )
                return maker2.create(exprs[0], exprs[1]);
            throw new ShaclException("Wrong number of argum ents expressions: expected 1 or 2, got "+exprs.length);
        };
        Call call = args->{
            if ( args.length == 1 )
                return function1.exec(args[0]);
            if ( args.length == 2 )
                return function2.exec(args[0], args[1]);
            throw exception("%s: Expected one or two arguments. Got %d", uri, args.length);
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // Arity 2
    private static <X> void entry2(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                   String uriName, Class<? extends Expr> implClass, String sparqlName,
                                   Create2<? extends Expr> maker, Function2 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 2 )
                throw new ShaclException("Wrong number of arguments expressions: expected 2, got "+exprs.length);
            return maker.create(exprs[0], exprs[1]);
        };
        Call call = args->{
            if ( args.length != 2 ) throw exception("%s: Expected two arguments. Got %d", uri, args.length);
            return function.exec(args[0], args[1]);
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // Arity 2 or 3 - switch by arity
    private static <X> void entry23(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                    String uriName, Class<? extends Expr> implClass, String sparqlName,
                                    Create2<? extends Expr> maker2, Function2 function2,
                                    Create3<? extends Expr> maker3, Function3 function3) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length == 2)
                return maker2.create(exprs[0], exprs[1]);
            if ( exprs.length == 3 )
                return maker3.create(exprs[0], exprs[1], exprs[2]);
            throw new ShaclException("Wrong number of arguments expressions: expected 2 or 3, got "+exprs.length);
        };
        Call call = args->{
            if ( args.length == 2 )
                return function2.exec(args[0], args[1]);
            if ( args.length == 3 )
                return function3.exec(args[0], args[1], args[2]);
            throw exception("%s: Expected two or three arguments. Got %d", uri, args.length);
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // Arity 3
    private static <X> void entry3(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                   String uriName, Class<? extends Expr> implClass, String sparqlName,
                                   Create3<? extends Expr> maker, Function3 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 3 )
                throw new ShaclException("Wrong number of arguments expressions: expected 3, got "+exprs.length);
            return maker.create(exprs[0], exprs[1], exprs[2]);
        };
        Call call = args->{
            if ( args.length != 1 ) throw exception("%s: Expected three arguments. Got %d", uri, args.length);
            return function.exec(args[0], args[1], args[2]);
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // Arity 3 or 4, extends 3 to 4 by a null argument
    private static <X> void entry34(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                    String uriName, Class<? extends Expr> implClass, String sparqlName,
                                    Create3<? extends Expr> maker3, Function3 function3,
                                    Create4<? extends Expr> maker4, Function4 function4) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length == 3)
                return maker4.create(exprs[0], exprs[1], exprs[2],null);
            if ( exprs.length == 4 )
                return maker4.create(exprs[0], exprs[1], exprs[3], exprs[4]);
            throw new ShaclException("Wrong number of arguments expressions: expected 3 or 4, got "+exprs.length);
        };
        Call call = args->{
            if ( args.length == 3 )
                return function3.exec(args[0], args[1], args[2]);
            if ( args.length == 4 )
                return function4.exec(args[0], args[1], args[2], args[3]);
            throw exception("%s: Expected two or three arguments. Got %d", uri, args.length);
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    private static <X> void entry4(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                   String uriName, Class<? extends Expr> implClass, String sparqlName,
                                   Create4<? extends Expr> maker, Function4 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 4 )
                throw new ShaclException("Wrong number of arguments expressions: expected 4, got "+exprs.length);
            return maker.create(exprs[0], exprs[1], exprs[2], exprs[3]);
        };
        Call call = args->{
            if ( args.length != 4 )
                throw exception("%s: Expected four arguments. Got %d", uri, args.length);
            return function.exec(args[0], args[1], args[2], args[3]);
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // N-ary
    private static <X> void entryN(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                   String uriName, Class<? extends Expr> implClass,
                                   String sparqlName, CreateN<? extends Expr> maker, FunctionN function) {

        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 4 )
                throw new ShaclException("Wrong number of arguments expressions: expected 4, got "+exprs.length);
            ExprList exprList = ExprList.create(exprs);
            return maker.create(exprList);
        };
        Call call = args->{
            return function.exec(List.of(args));
        };
        mapDispatch.put(uri, call);
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    // Does not have to be a SPARQL FunctionForm - can be RDF only.

    static <X extends Expr> void entryFunctionForm1(Map<String, CallFF> mapDispatchFF,
                                                    Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                                    String uriName, Class<? extends Expr> implClass, String sparqlName,
                                                    Create1<X> maker, FunctionalForm1 functionForm1) {
        String uri = expandName(uriName);
        if ( maker != null ) {
            Build build = (u, exprs) ->{
                if ( exprs.length != 2 )
                    throw new ShaclException("Wrong number of arguments expressions: expected 1, got "+exprs.length);
                return maker.create(exprs[0]);
            };
            mapBuild.put(uri, build);
        }
        CallFF call = (graph, node, env, row, args) -> {
            if ( args.length == 1 )
                return functionForm1.exec(graph, node, env, row, args[0]);
            throw exception("%s: Expected one argument. Got %d", uri, args.length);
        };
        mapDispatchFF.put(uri, call);
        if ( implClass != null )
            mapFunctionURI.put(implClass, uri);
    }

    static <X extends Expr> void entryFunctionForm2(Map<String, CallFF> mapDispatchFF,
                                                    Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                                    String uriName, Class<? extends Expr> implClass, String sparqlName,
                                                    Create2<X> maker, FunctionalForm2 functionForm2) {
        String uri = expandName(uriName);
        if ( maker != null ) {
            Build build = (u, exprs) ->{
                if ( exprs.length != 2 )
                    throw new ShaclException("Wrong number of arguments expressions: expected 2, got "+exprs.length);
                return maker.create(exprs[0], exprs[1]);
            };
            mapBuild.put(uri, build);
        }
        CallFF call = (graph, node, env, row, args) -> {
            if ( args.length == 2 )
                return functionForm2.exec(graph, node, env, row, args[0], args[1]);
            throw exception("%s: Expected two arguments. Got %d", uri, args.length);
        };
        mapDispatchFF.put(uri, call);
        if ( implClass != null )
            mapFunctionURI.put(implClass, uri);
    }

    static <X extends Expr> void entryFunctionForm3(Map<String, CallFF> mapDispatchFF,
                                                    Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                                    String uriName, Class<? extends Expr> implClass, String sparqlName,
                                                    Create3<X> maker, FunctionalForm3 functionForm3) {
        String uri = expandName(uriName);
        if ( maker != null ) {
            Build build = (u, exprs) ->{
                if ( exprs.length != 3 )
                    throw new ShaclException("Wrong number of arguments expressions: expected 3, got "+exprs.length);
                return maker.create(exprs[0], exprs[1], exprs[2]);
            };
            mapBuild.put(uri, build);
        }
        CallFF call = (graph, node, env, row, args) -> {
            if ( args.length == 3 )
                return functionForm3.exec(graph, node, env, row, args[0], args[1], args[2]);
            throw exception("%s: Expected three arguments. Got %d", uri, args.length);
        };
        mapDispatchFF.put(uri, call);
        if ( implClass != null )
            mapFunctionURI.put(implClass, uri);
    }

    static <X extends Expr> void entryFunctionFormN(Map<String, CallFF> mapDispatchFF,
                                                    Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                                    String uriName, Class<? extends Expr> implClass, String sparqlName,
                                                    CreateN<X> maker, FunctionalFormN functionFormN) {
        String uri = expandName(uriName);
        if ( maker != null ) {
            Build build = (u, exprs) ->{
                ExprList exprList = ExprList.create(exprs);
                return maker.create(exprList);
            };
            mapBuild.put(uri, build);
        }
        CallFF call = (graph, node, env, row, args) -> {
            List<Node> listArgs = List.of(args);
            return functionFormN.exec(graph, node, env, row, listArgs);
        };
        mapDispatchFF.put(uri, call);
        if ( implClass != null )
            mapFunctionURI.put(implClass, uri);
    }

    private static RuntimeException exception(String format, Object...args) {
        String msg = String.format(format, args);
        return new SPARQLEvalException(msg);
    }

    interface Build { Expr build(String uri, Expr... expr);}

    interface Create0<X> { X create(); }
    interface Create1<X> { X create(Expr expr); }
    interface Create2<X> { X create(Expr expr1, Expr expr2); }
    interface Create3<X> { X create(Expr expr1, Expr expr2, Expr expr3); }
    interface Create4<X> { X create(Expr expr1, Expr expr2, Expr expr3, Expr expr4); }
    interface CreateN<X> { X create(ExprList exprs); }

    // Dispatch function
    interface Call { NodeValue exec(NodeValue... nv); }
    interface Function0 { NodeValue exec(); }
    interface Function1 { NodeValue exec(NodeValue nv); }
    interface Function2 { NodeValue exec(NodeValue nv1, NodeValue nv2); }
    interface Function3 { NodeValue exec(NodeValue nv1, NodeValue nv2, NodeValue nv3); }
    interface Function4 { NodeValue exec(NodeValue nv1, NodeValue nv2, NodeValue nv3, NodeValue nv4); }
    interface FunctionN { NodeValue exec(List<NodeValue> nvList); }

    // Dispatch functional form
    interface CallFF { NodeValue execFF(Graph graph, Node node, FunctionEnv env, Binding row, Node...args); }
    interface FunctionalForm0 { NodeValue exec(Graph graph, Node node, FunctionEnv env, Binding row); }
    interface FunctionalForm1 { NodeValue exec(Graph graph, Node node, FunctionEnv env, Binding row, Node arg1); }
    interface FunctionalForm2 { NodeValue exec(Graph graph, Node node, FunctionEnv env, Binding row, Node arg1, Node arg2); }
    interface FunctionalForm3 { NodeValue exec(Graph graph, Node node, FunctionEnv env, Binding row, Node arg1, Node arg2, Node arg3); }
    interface FunctionalForm4 { NodeValue exec(Graph graph, Node node, FunctionEnv env, Binding row, Node arg1, Node arg2, Node arg3, Node arg4); }
    interface FunctionalFormN { NodeValue exec(Graph graph, Node node, FunctionEnv env, Binding row, List<Node> args); }
}