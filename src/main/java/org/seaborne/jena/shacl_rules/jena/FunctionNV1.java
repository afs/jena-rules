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

package org.seaborne.jena.shacl_rules.jena;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionBase1;
import org.apache.jena.sparql.function.FunctionFactory;

/**
 * Take a {@link java.util.function.Function} and provide it for
 * ARQ expression execution.
 *
 */
public class FunctionNV1 extends FunctionBase1 implements FunctionFactory {

    public interface Impl { NodeValue exec(NodeValue nv1); }

    private final Impl function;

    public FunctionNV1(Impl function) {
        this.function = function;
    }

    @Override
    public NodeValue exec(NodeValue v) {
        return function.exec(v);
    }

    @Override
    public Function create(String uri) {
        // FunctionFactory entry. The function is stateless so can be reused.
        return this;
    }
}
