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

import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.Prefixes;
import org.apache.jena.shared.PrefixMapping;

// -> Prefixes
public class Prefixes2 {
    // Or reverse arguments and call "putAll", or "??"

    /** Copy a {@link PrefixMap} into a {@link PrefixMap}. */
    public static void copyInto(PrefixMap source, PrefixMap destination) {
        destination.putAll(source);
    }

    /** Copy a {@link PrefixMap} into a {@link PrefixMapping}. */
    public static void copyInto(PrefixMap source, PrefixMapping destination) {
        destination.setNsPrefixes(Prefixes.adapt(source));
    }

    /** Copy a {@link PrefixMap} into a {@link PrefixMapping}. */
    public static void copyInto(PrefixMapping source, PrefixMap destination) {
        destination.putAll(source);
    }

    /** Copy a {@link PrefixMapping} into a {@link PrefixMapping}. */
    public static void copyInto(PrefixMapping source, PrefixMapping destination) {
        destination.setNsPrefixes(source);
    }
}
