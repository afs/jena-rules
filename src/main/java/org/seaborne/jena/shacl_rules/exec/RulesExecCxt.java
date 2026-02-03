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

package org.seaborne.jena.shacl_rules.exec;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;

/**
 * Rule execution environment.
 * <p>
 * This includes tracing support and a {@link Context}.
 */
public class RulesExecCxt implements FunctionEnv {
    // FunctionEnv is necessary for function evaluation but
    // SHACL Rules does not include SPARQL functions that
    // need the graph or dataset (e.g. EXISTS).

    //public static RuleExecCxt global = new RuleExecCxt();

    private IndentedWriter out = IndentedWriter.clone(IndentedWriter.stdout).setFlushOnNewline(true);

    private final Context context;

    public static RulesExecCxt create(Context context) {
        // Always isolate.
        // Be careful not to copy too many times!
        return new RulesExecCxt(context.copy());
    }

    private RulesExecCxt(Context context) {
        this.context = context;
    }

    @Override
    public Graph getActiveGraph() {
        throw new UnsupportedOperationException("RuleExecCxt.getActiveGraph");
    }

    @Deprecated
    @Override
    public DatasetGraph getDataset() {
        throw new UnsupportedOperationException("RuleExecCxt.getDataset");
    }

    @Override
    public Context getContext() {
        return context;
    }

    public void start() {}

    public void finish() {
        out.flush();
    }

    /** Debug rules evaluated. */
    public boolean DEBUG = false;
    /** Development : debug details */
    public boolean debug() { return DEBUG; }

    /** Trace rules evaluated. */
    public boolean TRACE = false;
    /** Application: trace rule execution */
    public boolean trace() { return TRACE; }

    public IndentedWriter out() { return out; }
}
