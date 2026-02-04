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

package org.seaborne.jena.shacl_rules.tuples0.rel;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RelStore {

    public Iterator<Rel> find(Rel rel);

    /** Get all the Rels matching a name*/
    public Stream<Rel> get(String relName);

    /** Does this RelStore have any rels of a given name? */
    public boolean containRel(String relName);

    public boolean matches(Rel rel);

    /* Contains exactly, no pattern matching */
    public boolean contains(Rel rel);

    public boolean isEmpty();

    public long size();

    public Stream<Rel> stream();

    /** Set equals */
    public static boolean equals(RelStore rs1, RelStore rs2) {
        Set<Rel> set1 = rs1.stream().collect(Collectors.toSet());
        Set<Rel> set2 = rs2.stream().collect(Collectors.toSet());
        return set1.equals(set2);
    }

    public static String toMultiLineString(RelStore relStore) {
        StringJoiner sj = new StringJoiner("\n + ", "++ ", "");
        relStore.stream().forEach(r -> sj.add(r.toString()));
        return sj.toString();
    }

    public static void print(PrintStream out, RelStore relStore) {
        boolean first = true;
        relStore.stream().forEach(r->{
            out.print("+ " );
            out.println(r);
        });
    }
}

