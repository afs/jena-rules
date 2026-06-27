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

package org.seaborne.jena.shacl_rules.tuples;

import java.io.OutputStream;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.PrefixMap;

public class TupleStoreWriter {

    public static void write(OutputStream out, TupleStore tupleStore, PrefixMap prefixMap) {
        try ( IndentedWriter iOut = new IndentedWriter(out) ) {
            write(iOut, prefixMap, tupleStore);
        }
    }

    public static void print(TupleStore tupleStore, PrefixMap prefixMap) {
        IndentedWriter iOut = IndentedWriter.stdout.clone();
        try ( iOut ) {
            write(iOut, prefixMap, tupleStore);
        }
    }

    public static void write(IndentedWriter iOut, PrefixMap prefixMap, TupleStore tupleStore) {
        NodeFormatter nFmt =  new NodeFormatterTTL(null, prefixMap);
        for ( Tuple tuple : tupleStore ) {
            write(iOut, nFmt, tuple);
        }
    }

    private static void write(IndentedWriter iOut, NodeFormatter nFmt, Tuple tuple) {
        iOut.print("$( ");
        boolean first = true;
        for ( Node n : tuple ) {
            if ( ! first )
                iOut.print(", ");
            else
                first = false;
            nFmt.format(iOut, n);
        }
        iOut.print(" )");
        iOut.println();
    }
}
