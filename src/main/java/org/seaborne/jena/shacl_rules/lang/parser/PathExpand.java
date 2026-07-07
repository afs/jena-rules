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

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.VarAlloc;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.Path;
import org.seaborne.jena.shacl_rules.RulesException;

class PathExpand {
    // See ARQ PathCompiler

    interface TripleReceiver { void accept(Node s, Node p, Node o); }

    // Seq and inverse and link
    public static void pathExpand(VarAlloc varAlloc, Node s, Path path, Node o, TripleReceiver tripleHandler) {
        if ( path instanceof P_Link pLink ) {
            Node p = pLink.getNode();
            tripleHandler.accept(s, p, o);
            return ;
        }

        if ( path instanceof P_Seq pSeq ) {
            Node v = varAlloc.allocVar();
            Path left = pSeq.getLeft();
            Path right = pSeq.getRight();
            pathExpand(varAlloc, s, left, v, tripleHandler);
            pathExpand(varAlloc, v, right, o, tripleHandler);
            return;
        }

        if ( path instanceof P_Inverse pInverse) {
            pathExpand(varAlloc, o, pInverse.getSubPath(), s, tripleHandler);
            return;
        }

        throw new RulesException("Path not supported: "+path);
    }
}
