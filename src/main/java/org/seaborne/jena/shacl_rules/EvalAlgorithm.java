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

package org.seaborne.jena.shacl_rules;


// Algorithm: Jacobi
//   Do each pass with respect to the previous round.
// Algorithm: Gauss-Seidel
//   Do each pass with growing inferred graph

public enum EvalAlgorithm {
    FWD_NAIVE("Naive")
    , FWD_NAIVE_JACOBI("Naive (Jacobi)")
    , FWD_NAIVE_GUASS_SEIDEL("Naive (GUASS_SEIDEL)")
    , FWD_SEMINAIVE("Seminaive")
    , BKD_NON_RECURSIVE_SLD("SLD (Non-recursive)")
    , BKD_QSQR("QSQR")
    , BKD_QSQI("QSQI")
//  , MAGIC("MagicSet")
    ;

    private final String displayName;

    private EvalAlgorithm(String string) { this.displayName = string; }

    public String displayName() {
        return displayName;
    }
}
