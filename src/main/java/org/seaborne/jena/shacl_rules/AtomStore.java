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

package org.seaborne.jena.shacl_rules;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;

/**
 * A tuple store can hold triples and named tuples.
 *
 */
public interface AtomStore {

    public void add(Triple triple);

    public Graph getGraph();

    public Iterator<Atom> find(Atom atomPattern);

    /** Get all the Atoms matching a name*/
    public Stream<Atom> get(String relName);

    /** Does this TupleStore have any atoms of a given name? */
    public boolean containAtom(String relName);

    public boolean matches(Atom rel);

    /* Contains exactly, no pattern matching */
    public boolean contains(Atom rel);

    public boolean isEmpty();

    public long size();

    public Stream<Atom> stream();

    /** Set equals */
    public static boolean equals(AtomStore rs1, AtomStore rs2) {
        Set<Atom> set1 = rs1.stream().collect(Collectors.toSet());
        Set<Atom> set2 = rs2.stream().collect(Collectors.toSet());
        return set1.equals(set2);
    }

    public static String toMultiLineString(AtomStore relStore) {
        StringJoiner sj = new StringJoiner("\n + ", "++ ", "");
        relStore.stream().forEach(r -> sj.add(r.toString()));
        return sj.toString();
    }

    public static void print(PrintStream out, AtomStore relStore) {
        boolean first = true;
        relStore.stream().forEach(r->{
            out.print("+ " );
            out.println(r);
        });
    }


}
