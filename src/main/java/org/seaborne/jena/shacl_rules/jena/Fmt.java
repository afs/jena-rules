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

package org.seaborne.jena.shacl_rules.jena;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.ExprUtils;

public class Fmt {
    // ExprUtils
    public static String fmtSPARQL(Expr expr, PrefixMapping prefixMapping) {
        SerializationContext sCxt  = new SerializationContext(prefixMapping);
        IndentedLineBuffer buff = new IndentedLineBuffer();
        ExprUtils.fmtSPARQL(buff, expr, sCxt);
        return buff.asString();
    }

    public static String fmtSPARQL(Expr expr, PrefixMap prefixMap) {
        /// XXX
        SerializationContext sCxt  = new SerializationContext(Prefixes.adapt(prefixMap));
        IndentedLineBuffer buff = new IndentedLineBuffer();
        ExprUtils.fmtSPARQL(buff, expr, sCxt);
        return buff.asString();
    }

}
