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

import org.seaborne.jena.srl.exec.EngineType;
import org.seaborne.jena.srl.exec.RulesEngineRegistry;

/**
 * System settings.
 */
public class SysJenaRules {

    /** System default {@link EngineType} */
    public static final EngineType dftEngineType = EngineType.SIMPLE;

    /** Allow unsafe recursive rules (development only) */
    public /*final*/ static boolean allowUnsafe = false;


    /**
     * Role triples for RDF syntax.
     * Role triples are {@code srl:triplePattern} and {@code srl:tripleTemplate}.
     * Otherwise, a triple encoding (a blank node and three triples srl:subject/srl:predicate/srl:object)
     * is used and whether it is a pattern or template triple is determined by location.
     * Role triples are preferred.
     */
    public static final boolean useRoleTriples = true;

    public static void init() {
        RulesEngineRegistry.init();
    }
}
