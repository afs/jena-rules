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

package org.seaborne.jena.shacl_rules.expr;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jena.sparql.function.FunctionFactory;
import org.apache.jena.sparql.function.FunctionRegistry;

// Look in two places.
// Add to the second FunctionRegistry
// The first is never changed.
class FunctionRegistry2 extends FunctionRegistry {

    /**
     * Return a {@link FunctionRegistry} that keeps modification separate
     * from the unchanged argument {@code registry}.
     */
    public static FunctionRegistry create(FunctionRegistry registry) {
        return new FunctionRegistry2(registry, new FunctionRegistry());
    }

    private final FunctionRegistry registry1;
    private final FunctionRegistry registry2;
    private final Set<String> removals = ConcurrentHashMap.newKeySet();

    private FunctionRegistry2(FunctionRegistry registry1, FunctionRegistry registry2) {
        this.registry1 = registry1;
        this.registry2 = registry2;
    }

    @Override
    public FunctionRegistry put(String uri, FunctionFactory f) {
        if ( registry1.isRegistered(uri) )
            return this;
        registry2.put(uri, f);
        removals.remove(uri);
        return this;
    }

    @Override
    public FunctionFactory getFunctionFactory(String uri) {
        if ( removals.contains(uri) )
             return null;
        FunctionFactory ff1 = registry1.getFunctionFactory(uri);
        if ( ff1 != null ) {
            return ff1;
        }
        return registry2.getFunctionFactory(uri);
    }

    @Override
    public void remove(String uri) {
        if ( registry1.isRegistered(uri) )
            removals.add(uri);
        registry2.remove(uri);
    }

    @Override
    public boolean isRegistered(String uri) {
        if ( removals.contains(uri) )
            return false;
        return registry2.isRegistered(uri) || registry1.isRegistered(uri);
    }

    @Override
    public Iterator<String> keys() {
        Set<String> keys = new HashSet<>();
        registry1.keys().forEachRemaining(keys::add);
        registry2.keys().forEachRemaining(keys::add);
        removals.forEach(keys::remove);
        return keys.iterator();
    }
}