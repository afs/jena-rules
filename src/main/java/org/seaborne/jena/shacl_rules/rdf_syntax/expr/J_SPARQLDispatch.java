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
import java.util.Objects;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * Dispatch to SPARQL function by URI.
 */
public class J_SPARQLDispatch {

    static final String NS0 = J_SPARQLFuncOp.NS.substring(0, J_SPARQLFuncOp.NS.length()-1);

    public static NodeValue exec(String uri, List<NodeValue>args) {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(args);

        NodeValue[] a = args.toArray(NodeValue[]::new);
        return exec(uri,a);
    }

    public static NodeValue exec(String uri, NodeValue...args) {
        if ( uri.startsWith("#") ) {
            // Short name.
            uri = J_SPARQLDispatch.NS0+uri;
        }

        Call call = getDispatchMap().get(uri);
        if ( call == null )
            throw new SPARQLEvalException("No such function: "+uri);
        return call.exec(args);
    }

    private static RuntimeException exception(String format, Object...args) {
        String msg = String.format(format, args);
        return new SPARQLEvalException(msg);
    }

    static void register(Map<String, Call> map, String uri, Function0 function) {
        Call call = args->{
            if ( args.length != 0 ) throw exception("%s: Expected zero arguments. Got %d", uri, args.length);
            return function.exec();
        };
        registerCall(map, uri, call);
    }

    static void register(Map<String, Call> map, String uri, Function1 function) {
        Call call = args->{
            if ( args.length != 1 ) throw exception("%s: Expected one argument. Got %d", uri, args.length);
            return function.exec(args[0]);
        };
        registerCall(map, uri, call);
    }

    static void register(Map<String, Call> map, String uri, Function2 function) {
        Call call = args->{
            if ( args.length != 2 ) throw exception("%s: Expected two arguments. Got %d", uri, args.length);
            return function.exec(args[0], args[1]);
        };
        registerCall(map, uri, call);
    }

    static void register(Map<String, Call> map, String uri, Function3 function) {
        Call call = args->{
            if ( args.length != 3 ) throw exception("%s: Expected three arguments. Got %d", uri, args.length);
            return function.exec(args[0], args[1], args[2]);
        };
        registerCall(map, uri, call);
    }

    static void register(Map<String, Call> map, String uri, Function4 function) {
        Call call = args->{
            if ( args.length != 3 ) throw exception("%s: Expected  arguments. Got %d", uri, args.length);
            return function.exec(args[0], args[1], args[2], args[3]);
        };
        registerCall(map, uri, call);
    }

    // Switch on arity
    static void register(Map<String, Call> map, String uri, Function1 function1, Function2 function2) {
        Call call = args->{
            if ( args.length == 1 )
                return function1.exec(args[0]);
            if ( args.length == 2 )
                return function2.exec(args[0], args[1]);
            throw exception("%s: Expected one or two arguments. Got %d", uri, args.length);
        };
        registerCall(map, uri, call);
    }

    // Switch on arity
    static void register(Map<String, Call> map, String uri, Function2 function2, Function3 function3) {
        Call call = args->{
            if ( args.length == 2 )
                return function2.exec(args[0], args[1]);
            if ( args.length == 3 )
                return function3.exec(args[0], args[1], args[2]);
            throw exception("%s: Expected two or three arguments. Got %d", uri, args.length);
        };
        registerCall(map, uri, call);
    }

    // Switch on arity
    static void register(Map<String, Call> map, String uri, FunctionN function) {
        Call call = args->{
            return function.exec(args);
        };
        registerCall(map, uri, call);
    }

    static void registerCall(Map<String, Call> map, String localName, Call call) {
        String uri = J_SPARQLFuncOp.NS+localName;
        Call oldCall = map.put(uri, call);
        if ( oldCall != null )
            throw new InternalErrorException("Multiple registration of "+uri);
    }

    private static class LazyDispatchMap {
        private static final Map<String, Call> INITIALIZED_MAP = buildDispatchMap();
        private static Map<String, Call> dispatchMap() {
            return INITIALIZED_MAP;
        }
    }

    static Map<String, Call> getDispatchMap() {
        return LazyDispatchMap.dispatchMap();
    }

    static Map<String, Call> buildDispatchMap() {
        Map<String, Call> map = new HashMap<>();

        // Move out/rename to "FunctionDispatch"
        // Add ARQ specials.

        register(map, "plus", J_SPARQLFuncOp::sparql_plus );
        register(map, "add", J_SPARQLFuncOp::sparql_plus );           // Alt name.
        register(map, "subtract", J_SPARQLFuncOp::sparql_subtract );
        register(map, "minus", J_SPARQLFuncOp::sparql_subtract );     // Alt name.
        register(map, "multiply", J_SPARQLFuncOp::sparql_multiply );
        register(map, "divide", J_SPARQLFuncOp::sparql_divide );

        register(map, "equals", J_SPARQLFuncOp::sparql_equals );
        register(map, "not-equals", J_SPARQLFuncOp::sparql_not_equals );
        register(map, "greaterThan", J_SPARQLFuncOp::sparql_greaterThan );
        register(map, "greaterThanOrEqual", J_SPARQLFuncOp::sparql_greaterThanOrEqual );
        register(map, "lessThan", J_SPARQLFuncOp::sparql_lessThan );
        register(map, "lessThanOrEqual", J_SPARQLFuncOp::sparql_lessThanOrEqual );

        register(map, "and", J_SPARQLFuncOp::sparql_function_and );
        register(map, "or", J_SPARQLFuncOp::sparql_function_or );
        register(map, "not", J_SPARQLFuncOp::sparql_function_not );

        register(map, "unary-minus", J_SPARQLFuncOp::sparql_unary_minus );
        register(map, "unary-plus", J_SPARQLFuncOp::sparql_unary_plus );

        register(map, "abs", J_SPARQLFuncOp::sparql_abs);
        register(map, "bnode", J_SPARQLFuncOp::sparql_bnode);
        register(map, "ceil", J_SPARQLFuncOp::sparql_ceil);

        // List arity
        register(map, "concat", (NodeValue[] args)->J_SPARQLFuncOp.sparql_concat(args));
        register(map, "contains", J_SPARQLFuncOp::sparql_contains);
        register(map, "datatype", J_SPARQLFuncOp::sparql_datatype);

        register(map, "encode", J_SPARQLFuncOp::sparql_encode);
        register(map, "floor", J_SPARQLFuncOp::sparql_floor);
        register(map, "haslang", J_SPARQLFuncOp::sparql_haslang);
        register(map, "haslangdir", J_SPARQLFuncOp::sparql_haslangdir);
        // Arity 1/2
        register(map, "iri", x->J_SPARQLFuncOp.sparql_iri(x), (x,b)->J_SPARQLFuncOp.arq_iri(x,b));
        register(map, "uri", x->J_SPARQLFuncOp.sparql_uri(x), (x,b)->J_SPARQLFuncOp.arq_uri(x,b));

        register(map, "isBlank", J_SPARQLFuncOp::sparql_isBlank);
        register(map, "isLiteral", J_SPARQLFuncOp::sparql_isLiteral);
        register(map, "isNumeric", J_SPARQLFuncOp::sparql_isNumeric);
        register(map, "isIRI", J_SPARQLFuncOp::sparql_isIRI);
        register(map, "isURI", J_SPARQLFuncOp::sparql_isURI);
        register(map, "lang", J_SPARQLFuncOp::sparql_lang);
        register(map, "langMatches", J_SPARQLFuncOp::sparql_langMatches);
        register(map, "langdir", J_SPARQLFuncOp::sparql_langdir);
        register(map, "lcase", J_SPARQLFuncOp::sparql_lcase);
        register(map, "ucase", J_SPARQLFuncOp::sparql_ucase);
        register(map, "now", J_SPARQLFuncOp::sparql_now);
        register(map, "rand", J_SPARQLFuncOp::sparql_rand);
        register(map, "regex", J_SPARQLFuncOp::sparql_regex);
        register(map, "replace", J_SPARQLFuncOp::sparql_replace);
        register(map, "round", J_SPARQLFuncOp::sparql_round);
        register(map, "sameTerm", J_SPARQLFuncOp::sparql_sameTerm);
        register(map, "sameValue", J_SPARQLFuncOp::sparql_sameValue);
        register(map, "uuid", J_SPARQLFuncOp::sparql_uuid);

        register(map, "year", J_SPARQLFuncOp::sparql_year);
        register(map, "month", J_SPARQLFuncOp::sparql_month);
        register(map, "day", J_SPARQLFuncOp::sparql_day);
        register(map, "hours", J_SPARQLFuncOp::sparql_hours);
        register(map, "minutes", J_SPARQLFuncOp::sparql_minutes);
        register(map, "seconds", J_SPARQLFuncOp::sparql_seconds);
        register(map, "tz", J_SPARQLFuncOp::sparql_tz);
        register(map, "timezone", J_SPARQLFuncOp::sparql_timezone);

        register(map, "subject", J_SPARQLFuncOp::sparql_subject);
        register(map, "object", J_SPARQLFuncOp::sparql_object);
        register(map, "predicate", J_SPARQLFuncOp::sparql_predicate);
        register(map, "isTriple", J_SPARQLFuncOp::sparql_isTriple);
        register(map, "triple", J_SPARQLFuncOp::sparql_triple);

        register(map, "md5", J_SPARQLFuncOp::sparql_md5);
        register(map, "sha1", J_SPARQLFuncOp::sparql_sha1);
        register(map, "sha224", J_SPARQLFuncOp::sparql_sha224);
        register(map, "sha256", J_SPARQLFuncOp::sparql_sha256);
        register(map, "sha384", J_SPARQLFuncOp::sparql_sha384);
        register(map, "sha512", J_SPARQLFuncOp::sparql_sha512);

        register(map, "str", J_SPARQLFuncOp::sparql_str);
        register(map, "strafter", J_SPARQLFuncOp::sparql_strafter);
        register(map, "strbefore", J_SPARQLFuncOp::sparql_strbefore);
        register(map, "strdt", J_SPARQLFuncOp::sparql_strdt);
        register(map, "strends", J_SPARQLFuncOp::sparql_strends);
        register(map, "strlang", J_SPARQLFuncOp::sparql_strlang);
        register(map, "strlangdir", J_SPARQLFuncOp::sparql_strlangdir);
        register(map, "strlen", J_SPARQLFuncOp::sparql_strlen);
        register(map, "strstarts", J_SPARQLFuncOp::sparql_strstarts);
        // Arity 2/3
        register(map, "substr", (s,x)->J_SPARQLFuncOp.sparql_substr(s,x), (s,x,y)->J_SPARQLFuncOp.sparql_substr(s,x,y));
        register(map, "struuid", J_SPARQLFuncOp::sparql_struuid);

        return Map.copyOf(map);
     }

    interface Call { NodeValue exec(NodeValue... nv); }
    interface Function0 { NodeValue exec(); }
    interface Function1 { NodeValue exec(NodeValue nv); }
    interface Function2 { NodeValue exec(NodeValue nv1, NodeValue nv2); }
    interface Function3 { NodeValue exec(NodeValue nv1, NodeValue nv2, NodeValue nv3); }
    interface Function4 { NodeValue exec(NodeValue nv1, NodeValue nv2, NodeValue nv3, NodeValue nv4); }
    interface FunctionN { NodeValue exec(NodeValue... nv); }
}
