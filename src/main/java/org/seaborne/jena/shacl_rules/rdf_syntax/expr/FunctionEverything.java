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

public class FunctionEverything {

    // ToDo
    // [ ] Complete SPARQLDispatch/SPARQLFuncOp -
    //  [ ] Include ARQ specials.  SPARQLFuncOp.arq_???
    //      SPARQLDispatch/SPARQLFuncOp :: Loose the iri2 , uri2 arity things
    // [ ] Formatting one line lists.

    // "Temporary" hack.
    static Map<Class<?>, String> mapFunctionURI = new HashMap<>();
    static String uriForExpr(Expr expr) {
        return mapFunctionURI.get(expr.getClass());
    }
//
//    public static void main(String...a) {
//        Map<String, Call> mapDispatch = new HashMap<>();
//        Map<String, Build> mapBuild = new HashMap<>();
//        table(mapDispatch, mapBuild);
//
////        String data = """
////PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
////PREFIX sh:     <http://www.w3.org/ns/shacl#>
////PREFIX sparql: <http://www.w3.org/ns/sparql#>
////
////[
////  rdf:type sh:SPARQLExpr;
////  sh:expr [
////    sparql:lessThan (
////      1
////      2
////    )
////  ]
////] .
////                """;
////        Graph graph = RDFParser.fromString(data, Lang.TTL).toGraph();
////        Node expression = G.getOnePO(graph, RDF.Nodes.type, V.sparqlExprClass);
////        Node x = G.getOneSP(graph, expression, V.expr);
////        Triple t = G.find(graph, x, null,null).next();
////        //System.out.println(pFunction);
//
//        String[] tests = {
//           "1",
//            //"URI('s')",
//            "strStarts('s', 'b')"
//            //,"regex()" // Not an expression
//            ,"strStarts(?x+'a', 123)"
//        };
//
//        for ( String s : tests ) {
//            System.out.println("== "+s);
//
//            Expr expr0 = ExprUtils.parse(s);
//
//            Graph graph = GraphFactory.createDefaultGraph();
//            DevShaclRules.addPrefixes(graph);
//
//            // to RDF
//            try {
//                SparqlNodeExpression.sparqlExprToRDF(graph, expr0);
//            } catch (Exception ex) {
//                System.out.println("** failed to convert to RDF: "+ex.getMessage());
//                continue;
//            }
//
////            Node topNode = SparqlNodeExpression.sparqlExprToRDF(graph, expr0);
//            RDFWriter.source(graph).format(RDFFormat.TURTLE).output(System.out);
//
//            // Find again.
//            Node topExpression = G.getOnePO(graph, RDF.Nodes.type, V.sparqlExprClass);
//
//            /// Top level.
//            Expr expr = RdfToExpr.buildExpr(mapBuild, graph);
////            try {
////                Node expression = G.getZeroOrOneSP(graph, topExpression, V.expr);
////                if ( expression != null )
////                    expr = buildExpr(mapBuild, graph, expression);
////                else
////                    expr = buildSparqlExpr(graph, topExpression);
////                if ( expr == null)
////                    throw new ShaclException("sh:expr not found (nor sh:sparqlExpr)");
////
////            } catch (Exception ex) {
////                System.out.println("** failed to rebuild expr: "+ex.getMessage());
////                ex.printStackTrace();
////                continue;
////            }
//
//            String expr$ = Fmt.fmtSPARQL(expr, graph.getPrefixMapping());
//            System.out.println("- - - -");
//            System.out.println("Expression: "+expr$);
//        }
//    }
//
    // ---------------------- Table building
    // Include class for checking? And build the table anyway.

    // These record everything we know about expressions

    // Rename by arity

//    private Map<String, String> map = Map.of(
//               "sparql", ARQConstants.sparqlPrefix,

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

    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, String uriName, Class<? extends Expr> implClass,
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

    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, String uriName, Class<? extends Expr> implClass,
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

    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, String uriName, Class<? extends Expr> implClass,
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


    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, String uriName, Class<? extends Expr> implClass,
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


    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, String uriName, Class<? extends Expr> implClass,
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
    private static <X> void entry(Map<String, Call> mapDispatch, Map<String, Build> mapBuild, String uriName, Class<? extends Expr> implClass,
                                  String sparqlName, CreateN<? extends Expr> maker, FunctionN function) {
        throw new NotImplemented();
    }



    // Build
    public interface Build { Expr build(String uri, Expr... expr); }
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

    enum Type { Function, FunctionalForm, Operator};

    // ToDo
    // [ ] Complete SPARQLDispatch/SPARQLFuncOp -
    // [ ] Include ARQ specials.  SPARQLFuncOp.arq_???
    //     SPARQLDispatch/SPARQLFuncOp :: Loose the iri2 , uri2 arity things
    // [ ] Special dispatch map.

    public static void table(Map<String, Call> mapDispatch, Map<String, Build> mapBuild) {


        // Merge eventually with
        //SPARQLDispatch.exec(null);
        // For now, join by URI.

        // Needed for building correct class.

        entry(mapDispatch, mapBuild, "sparql:iri", E_IRI.class, "IRI", E_IRI::new, (NodeValue x)->SPARQLFuncOp.sparql_iri(x));

        // Operators
        entry(mapDispatch, mapBuild, "sparql:plus", E_Add.class, "+", E_Add::new, SPARQLFuncOp::sparql_plus);
        entry(mapDispatch, mapBuild, "sparql:subtract", E_Subtract.class, "-", E_Subtract::new, SPARQLFuncOp::sparql_subtract);

        entry(mapDispatch, mapBuild, "sparql:plus", E_Add.class, "+", E_Add::new, SPARQLFuncOp::sparql_plus);
        entry(mapDispatch, mapBuild, "sparql:plus", E_Add.class, "+", E_Add::new, SPARQLFuncOp::sparql_plus);
        entry(mapDispatch, mapBuild, "sparql:plus", E_Add.class, "+", E_Add::new, SPARQLFuncOp::sparql_plus);

//        // ARQ
//        entry(mapDispatch, mapBuild, "arq:idiv", E_OpNumericIntegerDivide.class, "IDIV", E_OpNumericIntegerDivide::new, SPARQLFuncOp::arq_idiv);
//        // ARQ
//        entry(mapDispatch, mapBuild, "arq:mod", E_OpNumericMod.class, "MOD", E_OpNumericMod::new, SPARQLFuncOp::arq_mod);

        entry(mapDispatch, mapBuild, "sparql:unary-minus", E_UnaryMinus.class, "-", E_UnaryMinus::new, SPARQLFuncOp::sparql_unary_minus);
        entry(mapDispatch, mapBuild, "sparql:unary-plus", E_UnaryPlus.class, "+", E_UnaryPlus::new, SPARQLFuncOp::sparql_unary_plus );

        entry(mapDispatch, mapBuild, "sparql:greaterThan", E_GreaterThan.class, ">", E_GreaterThan::new, SPARQLFuncOp::sparql_greaterThan );
        entry(mapDispatch, mapBuild, "sparql:greaterThanOrEqual", E_GreaterThanOrEqual.class, ">=", E_GreaterThanOrEqual::new, SPARQLFuncOp::sparql_greaterThanOrEqual );
        entry(mapDispatch, mapBuild, "sparql:lessThan", E_LessThan.class, ">", E_LessThan::new, SPARQLFuncOp::sparql_lessThan);
        entry(mapDispatch, mapBuild, "sparql:lessThanOrEqual", E_LessThanOrEqual.class, ">=", E_LessThanOrEqual::new, SPARQLFuncOp::sparql_lessThanOrEqual );

        // Missing
//        entry(mapDispatch, mapBuild, "sparql:logical-and", E_LogicalAnd.class, "&&", E_LogicalAnd::new, SPARQLFuncOp::logical_and );
//        entry(mapDispatch, mapBuild, "sparql:logical-not", E_LogicalNot.class, "!", E_LogicalNot::new, SPARQLFuncOp::logical_not );
//        entry(mapDispatch, mapBuild, "sparql:logical-or", E_LogicalOr.class, "||", E_LogicalOr::new, SPARQLFuncOp::logical_or );

//        entry(mapDispatch, mapBuild, "sparql:in", E_OneOf.class, "IN", E_OneOf::new, SPARQLFuncOp::sparql_in );
//        entry(mapDispatch, mapBuild, "sparql:not-in", E_NotOneOf.class, "NOT IN", E_NotOneOf::new, SPARQLFuncOp::not_in );

        // URI function call.
//        entry(mapDispatch, mapBuild, "sparql:function", E_Function.class, "", E_Function::new, SPARQLFuncOp::function );

        // Functional forms (not functions)

//        entry(mapDispatch, mapBuild, "sparql:bound", E_Bound.class, "BOUND", E_Bound::new, SPARQLFuncOp::bound );
//        entry(mapDispatch, mapBuild, "sparql:coalesce", E_Coalesce.class, "COALESCE", E_Coalesce::new, SPARQLFuncOp::coalesce );
////**        entry(mapDispatch, mapBuild, "sparql:if", E_Conditional.class, "IF", E_Conditional::new, SPARQLFuncOp::if );

//        entry(mapDispatch, mapBuild, "sparql:filter-exists", E_Exists.class, "EXISTS", E_Exists::new, SPARQLFuncOp::filter-exists );
//        entry(mapDispatch, mapBuild, "sparql:filter-not-exists", E_NotExists.class, "NOT EXISTS", E_NotExists::new, SPARQLFuncOp::filter-not-exists );

        entry(mapDispatch, mapBuild, "sparql:now", E_Now.class, "NOW", E_Now::new, SPARQLFuncOp::sparql_now );

        // RDF Term related

        entry(mapDispatch, mapBuild, "sparql:haslangdir", E_HasLangDir.class, "hasLANGDIR", E_HasLangDir::new, SPARQLFuncOp::sparql_haslangdir );
        entry(mapDispatch, mapBuild, "sparql:haslang", E_HasLang.class, "hasLANG", E_HasLang::new, SPARQLFuncOp::sparql_haslang );
        entry(mapDispatch, mapBuild, "sparql:langdir", E_LangDir.class, "LANGDIR", E_LangDir::new, SPARQLFuncOp::sparql_langdir );
        entry(mapDispatch, mapBuild, "sparql:lang", E_Lang.class, "LANG", E_Lang::new, SPARQLFuncOp::sparql_lang );
        entry(mapDispatch, mapBuild, "sparql:langMatches", E_LangMatches.class, "LANGMATCHES", E_LangMatches::new, SPARQLFuncOp::sparql_langMatches );
        entry(mapDispatch, mapBuild, "sparql:datatype", E_Datatype.class, "DATATYPE", E_Datatype::new, SPARQLFuncOp::sparql_datatype );

        entry(mapDispatch, mapBuild, "sparql:strlangdir", E_StrLangDir.class, "STRLANGDIR", E_StrLangDir::new, SPARQLFuncOp::sparql_strlangdir );
        entry(mapDispatch, mapBuild, "sparql:strlang", E_StrLang.class, "STRLANG", E_StrLang::new, SPARQLFuncOp::sparql_strlang );

//        entry(mapDispatch, mapBuild, "arq:uri2", E_URI2.class, "URI2", E_URI2::new, SPARQLFuncOp::sparql_arq_uri2 );

        // E_URI extends E_IRI : Needs to be split up to compile
        Create1<E_IRI> iri1 = x->new E_IRI(x);
        Create1<E_IRI> uri1 = x->new E_URI(x);
        entry(mapDispatch, mapBuild, "sparql:iri", E_IRI.class, "IRI", iri1, x->SPARQLFuncOp.sparql_uri(x) );
        entry(mapDispatch, mapBuild, "sparql:uri", E_URI.class, "URI", uri1, x->SPARQLFuncOp.sparql_uri(x) );

        entry(mapDispatch, mapBuild, "sparql:isBlank", E_IsBlank.class, "isBlank", E_IsBlank::new, SPARQLFuncOp::sparql_isBlank );
        entry(mapDispatch, mapBuild, "sparql:isIRI", E_IsIRI.class, "isIRI", E_IsIRI::new, SPARQLFuncOp::sparql_isIRI );
        entry(mapDispatch, mapBuild, "sparql:isURI", E_IsURI.class, "isURI", E_IsURI::new, SPARQLFuncOp::sparql_isURI );
        entry(mapDispatch, mapBuild, "sparql:isLiteral", E_IsLiteral.class, "isLITERAL", E_IsLiteral::new, SPARQLFuncOp::sparql_isLiteral );
        entry(mapDispatch, mapBuild, "sparql:isNumeric", E_IsNumeric.class, "isNUMERIC", E_IsNumeric::new, SPARQLFuncOp::sparql_isNumeric );
        entry(mapDispatch, mapBuild, "sparql:isTriple", E_IsTriple.class, "isTRIPLE", E_IsTriple::new, SPARQLFuncOp::sparql_isTriple );

//        entry(mapDispatch, mapBuild, "sparql:iri", E_IRI.class, "IRI", x->new E_IRI(x), x->SPARQLFuncOp.sparql_iri(x) );
//        entry(mapDispatch, mapBuild, "sparql:uri", E_URI.class, "URI", x->new E_URI(x), x->SPARQLFuncOp.sparql_uri(x) );

//      entry(mapDispatch, mapBuild, "arq:iri2", E_URI2.class, "URI", E_IRI2::new, (x,y)->SPARQLFuncOp.sparql_iri(x,y) );
//      entry(mapDispatch, mapBuild, "arq:uri2", E_URI2.class, "URI", E_IRI2::new, (x,y)->SPARQLFuncOp.sparql_iri(x,y) );

        entry(mapDispatch, mapBuild, "sparql:strdt", E_StrDatatype.class, "STRDT", E_StrDatatype::new, SPARQLFuncOp::sparql_strdt );

        entry(mapDispatch, mapBuild, "sparql:bnode", E_BNode.BNode0.class, "BNODE", E_BNode::create, SPARQLFuncOp::sparql_bnode );
        entry(mapDispatch, mapBuild, "sparql:bnode", E_BNode.BNode1.class, "BNODE", E_BNode::create, SPARQLFuncOp::sparql_bnode );

        entry(mapDispatch, mapBuild, "sparql:sameTerm", E_SameTerm.class, "sameTERM", E_SameTerm::new, SPARQLFuncOp::sparql_sameTerm );
        entry(mapDispatch, mapBuild, "sparql:sameValue", E_SameValue.class, "sameVALUE", E_SameValue::new, SPARQLFuncOp::sparql_sameValue );

        // Datetime

        entry(mapDispatch, mapBuild, "sparql:year", E_DateTimeYear.class, "YEAR", E_DateTimeYear::new, SPARQLFuncOp::sparql_year );
        entry(mapDispatch, mapBuild, "sparql:month", E_DateTimeMonth.class, "MONTH", E_DateTimeMonth::new, SPARQLFuncOp::sparql_month );
        entry(mapDispatch, mapBuild, "sparql:day", E_DateTimeDay.class, "DAY", E_DateTimeDay::new, SPARQLFuncOp::sparql_day );
        entry(mapDispatch, mapBuild, "sparql:hours", E_DateTimeHours.class, "HOURS", E_DateTimeHours::new, SPARQLFuncOp::sparql_hours );
        entry(mapDispatch, mapBuild, "sparql:minutes", E_DateTimeMinutes.class, "MINUTES", E_DateTimeMinutes::new, SPARQLFuncOp::sparql_minutes );
        entry(mapDispatch, mapBuild, "sparql:seconds", E_DateTimeSeconds.class, "SECONDS", E_DateTimeSeconds::new, SPARQLFuncOp::sparql_seconds );
        entry(mapDispatch, mapBuild, "sparql:timezone", E_DateTimeTimezone.class, "TIMEZONE", E_DateTimeTimezone::new, SPARQLFuncOp::sparql_timezone );
        entry(mapDispatch, mapBuild, "sparql:tz", E_DateTimeTZ.class, "TZ", E_DateTimeTZ::new, SPARQLFuncOp::sparql_tz );
        // ARQ
//        entry(mapDispatch, mapBuild, "arq:adjust", E_AdjustToTimezone.class, "ADJUST", E_AdjustToTimezone::new, SPARQLFuncOp::sparql_ );

        // Numerics

        entry(mapDispatch, mapBuild, "sparql:abs", E_NumAbs.class, "ABS", E_NumAbs::new, SPARQLFuncOp::sparql_abs );
        entry(mapDispatch, mapBuild, "sparql:ceil", E_NumCeiling.class, "CEIL", E_NumCeiling::new, SPARQLFuncOp::sparql_ceil );
        entry(mapDispatch, mapBuild, "sparql:floor", E_NumFloor.class, "FLOOR", E_NumFloor::new, SPARQLFuncOp::sparql_floor );
        entry(mapDispatch, mapBuild, "sparql:round", E_NumRound.class, "ROUND", E_NumRound::new, SPARQLFuncOp::sparql_round );
        entry(mapDispatch, mapBuild, "sparql:rand", E_Random.class, "RAND", E_Random::new, SPARQLFuncOp::sparql_rand );

        // Hash functions

        entry(mapDispatch, mapBuild, "sparql:md5", E_MD5.class, "MD5", E_MD5::new, SPARQLFuncOp::sparql_md5 );
        entry(mapDispatch, mapBuild, "sparql:sha1", E_SHA1.class, "SHA1", E_SHA1::new, SPARQLFuncOp::sparql_sha1 );
        entry(mapDispatch, mapBuild, "sparql:sha224", E_SHA224.class, "SHA224", E_SHA224::new, SPARQLFuncOp::sparql_sha224 );
        entry(mapDispatch, mapBuild, "sparql:sha256", E_SHA256.class, "SHA256", E_SHA256::new, SPARQLFuncOp::sparql_sha256 );
        entry(mapDispatch, mapBuild, "sparql:sha384", E_SHA384.class, "SHA384", E_SHA384::new, SPARQLFuncOp::sparql_sha384 );
        entry(mapDispatch, mapBuild, "sparql:sha512", E_SHA512.class, "SHA512", E_SHA512::new, SPARQLFuncOp::sparql_sha512 );

        // Strings

        entry(mapDispatch, mapBuild, "sparql:str", E_Str.class, "STR", E_Str::new, SPARQLFuncOp::sparql_str );
        entry(mapDispatch, mapBuild, "sparql:strlen", E_StrLength.class, "STRLEN", E_StrLength::new, SPARQLFuncOp::sparql_strlen );
        entry(mapDispatch, mapBuild, "sparql:regex", E_Regex.class, "REGEX", E_Regex::new, SPARQLFuncOp::sparql_regex );

        entry(mapDispatch, mapBuild, "sparql:strafter", E_StrAfter.class, "strAFTER", E_StrAfter::new, SPARQLFuncOp::sparql_strafter );
        entry(mapDispatch, mapBuild, "sparql:strbefore", E_StrBefore.class, "strBEFORE", E_StrBefore::new, SPARQLFuncOp::sparql_strbefore );
        entry(mapDispatch, mapBuild, "sparql:strstarts", E_StrStartsWith.class, "STRSTARTS", E_StrStartsWith::new, SPARQLFuncOp::sparql_strstarts );
        entry(mapDispatch, mapBuild, "sparql:strends", E_StrEndsWith.class, "STRENDS", E_StrEndsWith::new, SPARQLFuncOp::sparql_strends );
        entry(mapDispatch, mapBuild, "sparql:lcase", E_StrLowerCase.class, "LCASE", E_StrLowerCase::new, SPARQLFuncOp::sparql_lcase );
        // Arity 3/4
//        entry(mapDispatch, mapBuild, "sparql:replace", E_StrReplace.class, "REPLACE", E_StrReplace::new, SPARQLFuncOp::sparql_replace );
        entry(mapDispatch, mapBuild, "sparql:substr", E_StrSubstring.class, "SUBSTR", E_StrSubstring::new, SPARQLFuncOp::sparql_substr );
        entry(mapDispatch, mapBuild, "sparql:ucase", E_StrUpperCase.class, "UCASE", E_StrUpperCase::new, SPARQLFuncOp::sparql_ucase );
        // Arity N
//        entry(mapDispatch, mapBuild, "sparql:concat", E_StrConcat.class, "CONCAT", E_StrConcat::new, SPARQLFuncOp::sparql_concat );
        entry(mapDispatch, mapBuild, "sparql:contains", E_StrContains.class, "CONTAINS", E_StrContains::new, SPARQLFuncOp::sparql_contains );

//        entry(mapDispatch, mapBuild, "sparql:encode-for-uri", E_StrEncodeForURI.class, "ENCODE_FOR_URI", E_StrEncodeForURI::new, SPARQLFuncOp::sparql_encode_for_uri );

        // UUIDs

        entry(mapDispatch, mapBuild, "sparql:uuid", E_UUID.class, "UUID", E_UUID::new, SPARQLFuncOp::sparql_uuid );
        entry(mapDispatch, mapBuild, "sparql:struuid", E_StrUUID.class, "STRUUID", E_StrUUID::new, SPARQLFuncOp::sparql_struuid );

        // Triple terms

        entry(mapDispatch, mapBuild, "sparql:triple", E_TripleFn.class, "TRIPLE", E_TripleFn::new, SPARQLFuncOp::sparql_triple );
        entry(mapDispatch, mapBuild, "sparql:object", E_TripleObject.class, "OBJECT", E_TripleObject::new, SPARQLFuncOp::sparql_object );
        entry(mapDispatch, mapBuild, "sparql:predicate", E_TriplePredicate.class, "PREDICATE", E_TriplePredicate::new, SPARQLFuncOp::sparql_predicate );
        entry(mapDispatch, mapBuild, "sparql:subject", E_TripleSubject.class, "SUBJECT", E_TripleSubject::new, SPARQLFuncOp::sparql_subject );

        // ARQ

//        entry(mapDispatch, mapBuild, "arq:call", E_Call.class, "CALL", E_Call::new, SPARQLFuncOp:: );
//        entry(mapDispatch, mapBuild, "arq:cast", E_Cast.class, "CAST", E_Cast::new, SPARQLFuncOp:: );
//        entry(mapDispatch, mapBuild, "arq:version", E_Version.class, "VERISON", E_Version::new, SPARQLFuncOp:: );
    }
}