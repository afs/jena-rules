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

package org.seaborne.jena.srl.sys;

import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;

public class SysSRL {

    public static String NS = "http://jena.apache/org/srl#";
    public static String srlPrefix = "srl:";

    public static Context getContext() {
        return ARQ.getContext();
    }

    public static Symbol allocSymbol(String shortName) {
        // This must work even if initialization is happening.
        // Touching final constant explicit strings in ARQ is fine (compile time constants).
        if ( shortName.startsWith("arq:") )
            throw new ARQInternalErrorException("Symbol short name begins with the ARQ namespace prefix: " + shortName) ;
        if ( shortName.startsWith("http:") )
            throw new ARQInternalErrorException("Symbol short name begins with http: " + shortName) ;
        if ( shortName.startsWith("https:") )
            throw new ARQInternalErrorException("Symbol short name begins with https: " + shortName) ;
        return allocSymbol(NS, shortName) ;
    }

    public static Symbol allocSymbol(String base, String shortName) {
        return Symbol.create(base + shortName) ;
    }
}
