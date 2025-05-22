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
import java.util.Map;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.urifunctions.SPARQLFuncOp;


class FunctionEverything {
    // Replace SPARQLDispatch

    // ---------------------- Table building
    // Include class for checking? And build the table anyway.

    // These record everything we know about expressions

    // Rename by arity

//    private Map<String, String> map = Map.of(
//               "sparql", ARQConstants.sparqlPrefix,


    static Call getCall(String uri) {
        return mapDispatch().get(uri);
    }

    static Build getBuild(String uri) {
        return mapBuild().get(uri);
    }

    static String getUriForExpr(Expr expr) {
        return mapFunctionURI().get(expr.getClass());
    }

    static class LazyInit {
        private static Map<String, Call> mapDispatch = new HashMap<>();
        private static Map<String, Build> mapBuild = new HashMap<>();
        private static Map<Class<?>, String> mapFunctionURI = new HashMap<>();
        static {
            FunctionEverything.initTables(mapDispatch, mapBuild, mapFunctionURI);
        }
    }

    static Map<String, Call> mapDispatch() { return LazyInit.mapDispatch; }
    static Map<String, Build> mapBuild() { return LazyInit.mapBuild; }
    static Map<Class<?>, String> mapFunctionURI() { return LazyInit.mapFunctionURI; }

    private static PrefixMap prefixMap = PrefixMapFactory.create();
    static {
        prefixMap.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixMap.add("sh", "http://www.w3.org/ns/shacl#");
        prefixMap.add("sparql:", "http://www.w3.org/ns/sparql#");
        prefixMap.add("arq:", "http://jenba.apache.org/ARQ/function#") ; // XXX WHERE?
    }

    private static String expandName(String x) {
        String z = prefixMap.expand(x);
        return (z != null) ? z : x;
    }

    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                  String uriName, Class<? extends Expr> implClass,
                                  String sparqlName, Create0<? extends Expr> maker, Function0 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 0 )
                throw new ShaclException("Wrong number of arguments expressions: expected 0, got "+exprs.length);
            return (Expr)maker.create();
        };
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                  String uriName, Class<? extends Expr> implClass,
                                  String sparqlName, Create1<? extends Expr> maker, Function1 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 1 )
                throw new ShaclException("Wrong number of arguments expressions: expected 1, got "+exprs.length);
            return (Expr)maker.create(exprs[0]);
        };
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                  String uriName, Class<? extends Expr> implClass,
                                  String sparqlName, Create2<? extends Expr> maker, Function2 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 2 )
                throw new ShaclException("Wrong number of arguments expressions: expected 2, got "+exprs.length);
            return (Expr)maker.create(exprs[0], exprs[1]);
        };
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }


    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                  String uriName, Class<? extends Expr> implClass,
                                  String sparqlName, Create3<? extends Expr> maker, Function3 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 3 )
                throw new ShaclException("Wrong number of arguments expressions: expected 3, got "+exprs.length);
            return (Expr)maker.create(exprs[0], exprs[1], exprs[2]);
        };
        mapBuild.put(uri, build);
        mapFunctionURI.put(implClass, uri);
    }

    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                  String uriName, Class<? extends Expr> implClass,
                                  String sparqlName, Create4<? extends Expr> maker, Function4 function) {
        String uri = expandName(uriName);
        Build build = (u, exprs) ->{
            if ( exprs.length != 4 )
                throw new ShaclException("Wrong number of arguments expressions: expected 4, got "+exprs.length);
            return (Expr)maker.create(exprs[0], exprs[1], exprs[2], exprs[3]);
        };
        mapBuild.put(uri, build);
    }
    // N-ary
    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI,
                                  String uriName, Class<? extends Expr> implClass,
                                  String sparqlName, CreateN<? extends Expr> maker, FunctionN function) {
        throw new NotImplemented();
    }

    // Build
    interface Build { Expr build(String uri, Expr... expr); }
    interface Create0<X> { X create(); }
    interface Create1<X> { X create(Expr expr); }
    interface Create2<X> { X create(Expr expr1, Expr expr2); }
    interface Create3<X> { X create(Expr expr1, Expr expr2, Expr expr3); }
    interface Create4<X> { X create(Expr expr1, Expr expr2, Expr expr3, Expr expr4); }
    interface CreateN<X> { X create(Expr... expr); }

    // Dispatch
    public interface Call { NodeValue exec(NodeValue... nv); }
    interface Function0 { NodeValue exec(); }
    interface Function1 { NodeValue exec(NodeValue nv); }
    interface Function2 { NodeValue exec(NodeValue nv1, NodeValue nv2); }
    interface Function3 { NodeValue exec(NodeValue nv1, NodeValue nv2, NodeValue nv3); }
    interface Function4 { NodeValue exec(NodeValue nv1, NodeValue nv2, NodeValue nv3, NodeValue nv4); }
    interface FunctionN { NodeValue exec(NodeValue... nv); }

    public static void initTables(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, Map<Class<?>, String> mapFunctionURI) {
        // Merge eventually with
        //SPARQLDispatch.exec(null);
        // For now, join by URI.

        // Needed for building correct class.

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:iri", E_IRI.class, "IRI", E_IRI::new, (NodeValue x)->SPARQLFuncOp.sparql_iri(x));

        // Operators
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:plus", E_Add.class, "+", E_Add::new, SPARQLFuncOp::sparql_plus);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:subtract", E_Subtract.class, "-", E_Subtract::new, SPARQLFuncOp::sparql_subtract);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:multiply", E_Divide.class, "*", E_Divide::new, SPARQLFuncOp::sparql_multiply);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:divide", E_Divide.class, "/", E_Divide::new, SPARQLFuncOp::sparql_divide);

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:equals", E_Equals.class, "=", E_Equals::new, SPARQLFuncOp::sparql_equals);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:equals", E_NotEquals.class, "!=", E_Equals::new, SPARQLFuncOp::sparql_not_equals);

//        // ARQ
//        entry(mapDispatch, mapBuild, mapFunctionURI, "arq:idiv", E_OpNumericIntegerDivide.class, "IDIV", E_OpNumericIntegerDivide::new, SPARQLFuncOp::arq_idiv);
//        // ARQ
//        entry(mapDispatch, mapBuild, mapFunctionURI, "arq:mod", E_OpNumericMod.class, "MOD", E_OpNumericMod::new, SPARQLFuncOp::arq_mod);

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:unary-minus", E_UnaryMinus.class, "-", E_UnaryMinus::new, SPARQLFuncOp::sparql_unary_minus);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:unary-plus", E_UnaryPlus.class, "+", E_UnaryPlus::new, SPARQLFuncOp::sparql_unary_plus );

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:greaterThan", E_GreaterThan.class, ">", E_GreaterThan::new, SPARQLFuncOp::sparql_greaterThan );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:greaterThanOrEqual", E_GreaterThanOrEqual.class, ">=", E_GreaterThanOrEqual::new, SPARQLFuncOp::sparql_greaterThanOrEqual );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:lessThan", E_LessThan.class, ">", E_LessThan::new, SPARQLFuncOp::sparql_lessThan);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:lessThanOrEqual", E_LessThanOrEqual.class, ">=", E_LessThanOrEqual::new, SPARQLFuncOp::sparql_lessThanOrEqual );

        // Specials as functions
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:logical-and", E_LogicalAnd.class, "&&", E_LogicalAnd::new, SPARQLFuncOp::sparql_function_and);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:logical-not", E_LogicalNot.class, "!", E_LogicalNot::new, SPARQLFuncOp::sparql_function_not);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:logical-or", E_LogicalOr.class, "||", E_LogicalOr::new, SPARQLFuncOp::sparql_function_or);

//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:in", E_OneOf.class, "IN", E_OneOf::new, SPARQLFuncOp::sparql_in );
//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:not-in", E_NotOneOf.class, "NOT IN", E_NotOneOf::new, SPARQLFuncOp::not_in );

        // URI function call.
//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:function", E_Function.class, "", E_Function::new, SPARQLFuncOp::function );

        // Functional forms (not functions)

//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:bound", E_Bound.class, "BOUND", E_Bound::new, SPARQLFuncOp::bound );
//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:coalesce", E_Coalesce.class, "COALESCE", E_Coalesce::new, SPARQLFuncOp::coalesce );
////**        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:if", E_Conditional.class, "IF", E_Conditional::new, SPARQLFuncOp::if );

//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:filter-exists", E_Exists.class, "EXISTS", E_Exists::new, SPARQLFuncOp::filter-exists );
//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:filter-not-exists", E_NotExists.class, "NOT EXISTS", E_NotExists::new, SPARQLFuncOp::filter-not-exists );

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:now", E_Now.class, "NOW", E_Now::new, SPARQLFuncOp::sparql_now );

        // RDF Term related

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:haslangdir", E_HasLangDir.class, "hasLANGDIR", E_HasLangDir::new, SPARQLFuncOp::sparql_haslangdir );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:haslang", E_HasLang.class, "hasLANG", E_HasLang::new, SPARQLFuncOp::sparql_haslang );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:langdir", E_LangDir.class, "LANGDIR", E_LangDir::new, SPARQLFuncOp::sparql_langdir );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:lang", E_Lang.class, "LANG", E_Lang::new, SPARQLFuncOp::sparql_lang );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:langMatches", E_LangMatches.class, "LANGMATCHES", E_LangMatches::new, SPARQLFuncOp::sparql_langMatches );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:datatype", E_Datatype.class, "DATATYPE", E_Datatype::new, SPARQLFuncOp::sparql_datatype );

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:strlangdir", E_StrLangDir.class, "STRLANGDIR", E_StrLangDir::new, SPARQLFuncOp::sparql_strlangdir );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:strlang", E_StrLang.class, "STRLANG", E_StrLang::new, SPARQLFuncOp::sparql_strlang );

//        entry(mapDispatch, mapBuild, mapFunctionURI, "arq:uri2", E_URI2.class, "URI2", E_URI2::new, SPARQLFuncOp::sparql_arq_uri2 );

        // E_URI extends E_IRI : Needs to be split up to compile
        Create1<E_IRI> iri1 = x->new E_IRI(x);
        Create1<E_IRI> uri1 = x->new E_URI(x);
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:iri", E_IRI.class, "IRI", iri1, x->SPARQLFuncOp.sparql_uri(x) );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:uri", E_URI.class, "URI", uri1, x->SPARQLFuncOp.sparql_uri(x) );

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:isBlank", E_IsBlank.class, "isBlank", E_IsBlank::new, SPARQLFuncOp::sparql_isBlank );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:isIRI", E_IsIRI.class, "isIRI", E_IsIRI::new, SPARQLFuncOp::sparql_isIRI );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:isURI", E_IsURI.class, "isURI", E_IsURI::new, SPARQLFuncOp::sparql_isURI );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:isLiteral", E_IsLiteral.class, "isLITERAL", E_IsLiteral::new, SPARQLFuncOp::sparql_isLiteral );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:isNumeric", E_IsNumeric.class, "isNUMERIC", E_IsNumeric::new, SPARQLFuncOp::sparql_isNumeric );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:isTriple", E_IsTriple.class, "isTRIPLE", E_IsTriple::new, SPARQLFuncOp::sparql_isTriple );

//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:iri", E_IRI.class, "IRI", x->new E_IRI(x), x->SPARQLFuncOp.sparql_iri(x) );
//        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:uri", E_URI.class, "URI", x->new E_URI(x), x->SPARQLFuncOp.sparql_uri(x) );

//      entry(mapDispatch, mapBuild, mapFunctionURI, "arq:iri2", E_URI2.class, "URI", E_IRI2::new, (x,y)->SPARQLFuncOp.sparql_iri(x,y) );
//      entry(mapDispatch, mapBuild, mapFunctionURI, "arq:uri2", E_URI2.class, "URI", E_IRI2::new, (x,y)->SPARQLFuncOp.sparql_iri(x,y) );

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:strdt", E_StrDatatype.class, "STRDT", E_StrDatatype::new, SPARQLFuncOp::sparql_strdt );

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:bnode", E_BNode.BNode0.class, "BNODE", E_BNode::create, SPARQLFuncOp::sparql_bnode );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:bnode", E_BNode.BNode1.class, "BNODE", E_BNode::create, SPARQLFuncOp::sparql_bnode );

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:sameTerm", E_SameTerm.class, "sameTERM", E_SameTerm::new, SPARQLFuncOp::sparql_sameTerm );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:sameValue", E_SameValue.class, "sameVALUE", E_SameValue::new, SPARQLFuncOp::sparql_sameValue );

        // Datetime

        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:year", E_DateTimeYear.class, "YEAR", E_DateTimeYear::new, SPARQLFuncOp::sparql_year );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:month", E_DateTimeMonth.class, "MONTH", E_DateTimeMonth::new, SPARQLFuncOp::sparql_month );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:day", E_DateTimeDay.class, "DAY", E_DateTimeDay::new, SPARQLFuncOp::sparql_day );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:hours", E_DateTimeHours.class, "HOURS", E_DateTimeHours::new, SPARQLFuncOp::sparql_hours );
        entry(mapDispatch, mapBuild, mapFunctionURI, "sparql:minutes", E_DateTimeMinutes.class, "MINUTES", E_DateTimeMinutes::new, SPARQLFuncOp::sparql_minutes );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:seconds", E_DateTimeSeconds.class, "SECONDS", E_DateTimeSeconds::new, SPARQLFuncOp::sparql_seconds );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:timezone", E_DateTimeTimezone.class, "TIMEZONE", E_DateTimeTimezone::new, SPARQLFuncOp::sparql_timezone );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:tz", E_DateTimeTZ.class, "TZ", E_DateTimeTZ::new, SPARQLFuncOp::sparql_tz );
        // ARQ
//        entry(mapDispatch, mapBuild, mapFunctionURI,"arq:adjust", E_AdjustToTimezone.class, "ADJUST", E_AdjustToTimezone::new, SPARQLFuncOp::sparql_ );

        // Numerics

        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:abs", E_NumAbs.class, "ABS", E_NumAbs::new, SPARQLFuncOp::sparql_abs );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:ceil", E_NumCeiling.class, "CEIL", E_NumCeiling::new, SPARQLFuncOp::sparql_ceil );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:floor", E_NumFloor.class, "FLOOR", E_NumFloor::new, SPARQLFuncOp::sparql_floor );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:round", E_NumRound.class, "ROUND", E_NumRound::new, SPARQLFuncOp::sparql_round );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:rand", E_Random.class, "RAND", E_Random::new, SPARQLFuncOp::sparql_rand );

        // Hash functions

        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:md5", E_MD5.class, "MD5", E_MD5::new, SPARQLFuncOp::sparql_md5 );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha1", E_SHA1.class, "SHA1", E_SHA1::new, SPARQLFuncOp::sparql_sha1 );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha224", E_SHA224.class, "SHA224", E_SHA224::new, SPARQLFuncOp::sparql_sha224 );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha256", E_SHA256.class, "SHA256", E_SHA256::new, SPARQLFuncOp::sparql_sha256 );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha384", E_SHA384.class, "SHA384", E_SHA384::new, SPARQLFuncOp::sparql_sha384 );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:sha512", E_SHA512.class, "SHA512", E_SHA512::new, SPARQLFuncOp::sparql_sha512 );

        // Strings

        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:str", E_Str.class, "STR", E_Str::new, SPARQLFuncOp::sparql_str );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:strlen", E_StrLength.class, "STRLEN", E_StrLength::new, SPARQLFuncOp::sparql_strlen );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:regex", E_Regex.class, "REGEX", E_Regex::new, SPARQLFuncOp::sparql_regex );

        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:strafter", E_StrAfter.class, "strAFTER", E_StrAfter::new, SPARQLFuncOp::sparql_strafter );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:strbefore", E_StrBefore.class, "strBEFORE", E_StrBefore::new, SPARQLFuncOp::sparql_strbefore );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:strstarts", E_StrStartsWith.class, "STRSTARTS", E_StrStartsWith::new, SPARQLFuncOp::sparql_strstarts );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:strends", E_StrEndsWith.class, "STRENDS", E_StrEndsWith::new, SPARQLFuncOp::sparql_strends );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:lcase", E_StrLowerCase.class, "LCASE", E_StrLowerCase::new, SPARQLFuncOp::sparql_lcase );
        // Arity 3/4
//        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:replace", E_StrReplace.class, "REPLACE", E_StrReplace::new, SPARQLFuncOp::sparql_replace );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:substr", E_StrSubstring.class, "SUBSTR", E_StrSubstring::new, SPARQLFuncOp::sparql_substr );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:ucase", E_StrUpperCase.class, "UCASE", E_StrUpperCase::new, SPARQLFuncOp::sparql_ucase );
        // Arity N
//        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:concat", E_StrConcat.class, "CONCAT", E_StrConcat::new, SPARQLFuncOp::sparql_concat );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:contains", E_StrContains.class, "CONTAINS", E_StrContains::new, SPARQLFuncOp::sparql_contains );

//        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:encode-for-uri", E_StrEncodeForURI.class, "ENCODE_FOR_URI", E_StrEncodeForURI::new, SPARQLFuncOp::sparql_encode_for_uri );

        // UUIDs

        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:uuid", E_UUID.class, "UUID", E_UUID::new, SPARQLFuncOp::sparql_uuid );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:struuid", E_StrUUID.class, "STRUUID", E_StrUUID::new, SPARQLFuncOp::sparql_struuid );

        // Triple terms

        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:triple", E_TripleFn.class, "TRIPLE", E_TripleFn::new, SPARQLFuncOp::sparql_triple );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:object", E_TripleObject.class, "OBJECT", E_TripleObject::new, SPARQLFuncOp::sparql_object );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:predicate", E_TriplePredicate.class, "PREDICATE", E_TriplePredicate::new, SPARQLFuncOp::sparql_predicate );
        entry(mapDispatch, mapBuild, mapFunctionURI,"sparql:subject", E_TripleSubject.class, "SUBJECT", E_TripleSubject::new, SPARQLFuncOp::sparql_subject );

        // ARQ

//        entry(mapDispatch, mapBuild, mapFunctionURI,"arq:call", E_Call.class, "CALL", E_Call::new, SPARQLFuncOp:: );
//        entry(mapDispatch, mapBuild, mapFunctionURI,"arq:cast", E_Cast.class, "CAST", E_Cast::new, SPARQLFuncOp:: );
//        entry(mapDispatch, mapBuild, mapFunctionURI,"arq:version", E_Version.class, "VERISON", E_Version::new, SPARQLFuncOp:: );
    }
}