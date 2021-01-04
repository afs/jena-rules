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

package org.seaborne.jena.rules.api;


// Algorithm: Jacobi
//   Do each pass with respect to the previous round.
// Algorithm: Gauss-Seidel
public enum EngineType {
    // Default navive (used for tests).
    FWD_NAIVE
    , FWD_NAIVE_JACOBI
    , FWD_NAIVE_GUEASS_SEIDEL
    , FWD_SEMINAIVE
    , BKD_NON_RECURSIVE_SLD
    , BKD_QSQR
    , BKD_QSQI
//    , MAGIC

    }