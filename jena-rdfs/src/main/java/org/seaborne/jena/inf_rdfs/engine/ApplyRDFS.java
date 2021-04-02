/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package org.seaborne.jena.inf_rdfs.engine;

import java.util.Set;

import org.seaborne.jena.inf_rdfs.SetupRDFS;

/**
 * Apply a fixed set of inference rules to a 3-tuple.
 * This class is the core machinery of stream expansion of a data stream using an RDFS schema.
 * This is inference on the A-Box (the data) with respect to a fixed T-Box
 * (the vocabulary, ontology).
 * <p>
 * This class implements:
 * <ul>
 * <li>rdfs:subClassOf (transitive)</li>
 * <li>rdfs:subPropertyOf (transitive)</li>
 * <li>rdfs:domain</li>
 * <li>rdfs:range</li>
 * </ul>
 *
 * @see MatchRDFS MatchRDFS for the matching algorithm.
 */

public class ApplyRDFS<X, T> extends CxtInf<X,T>{
    // [RDFS] javadoc
    // Expanded in X space.

    public ApplyRDFS(SetupRDFS<X> setup, MapperX<X, T> mapper) {
        super(setup, mapper);
    }

    // TODO: ?? Other RDFS+
    //   rdfs:member
    //   list:member

    /**
     * Apply RDFS rules based on the 3-tuple.
     * This does not include the triple itself unless it is inferred.
     */
    public void infer(T tuple, Output<X> out) {
        infer(mapper.subject(tuple), mapper.predicate(tuple), mapper.object(tuple), out);
    }

    /**
     * Apply RDFS rules based on the 3-tuple.
     * This does not include the triple itself unless it is inferred.
     */
    public void infer(X s, X p, X o, Output<X> out) {
        // Inferred.
        subClass(s, p, o, out);
        subProperty(s, p, o, out);
        // domain() and range() also go through subclass processing.
        domain(s, p, o, out);
        range(s, p, o, out);
    }

    /** Any triple derived is sent to this method. */
    private void derive(X s, X p, X o, Output<X> out) {
        out.action(s,p,o);
    }

    // Rule extracts from Jena's RDFS rules etc/rdfs.rules

    /*
     * [rdfs8: (?a rdfs:subClassOf ?b), (?b rdfs:subClassOf ?c) -> (?a rdfs:subClassOf ?c)]
     * [rdfs9: (?x rdfs:subClassOf ?y), (?a rdf:type ?x) -> (?a rdf:type ?y)]
     */
    private void subClass(X s, X p, X o, Output<X> out) {
        if ( p.equals(rdfType) ) {
            Set<X> x = setup.getSuperClasses(o);
            x.forEach(c -> derive(s, rdfType, c, out));
            if ( setup.includeDerivedDataRDFS() ) {
                subClass(o, rdfsSubClassOf, o, out);    // Recurse
            }
        }
        if ( setup.includeDerivedDataRDFS() && p.equals(rdfsSubClassOf) ) {
            Set<X> superClasses = setup.getSuperClasses(o);
            superClasses.forEach(c -> derive(o, p, c, out));
            Set<X> subClasses = setup.getSubClasses(o);
            subClasses.forEach(c -> derive(c, p, o, out));
            derive(s, p, s, out);
            derive(o, p, o, out);
        }
    }

    /*
     * [rdfs5a: (?a rdfs:subPropertyOf ?b), (?b rdfs:subPropertyOf ?c) -> (?a rdfs:subPropertyOf ?c)]
     * [rdfs6: (?a ?p ?b), (?p rdfs:subPropertyOf ?q) -> (?a ?q ?b)]
     */
    private void subProperty(X s, X p, X o, Output<X> out) {
        Set<X> x = setup.getSuperProperties(p);
        x.forEach(p2 -> derive(s, p2, o, out));
        if ( setup.includeDerivedDataRDFS() ) {
            if ( ! x.isEmpty() )
                subProperty(p, rdfsSubPropertyOf, p, out);
            if ( p.equals(rdfsSubPropertyOf) ) {
                // ** RDFS extra
                Set<X> superProperties = setup.getSuperProperties(o);
                superProperties.forEach( c -> derive(o, p, c, out));
                Set<X> subProperties = setup.getSubProperties(o);
                subProperties.forEach(c -> derive(c, p, o, out));
                derive(s, p, s, out);
                derive(o, p, o, out);
            }
        }
    }

    /*
     * [rdfs2: (?p rdfs:domain ?c) -> [(?x rdf:type ?c) <- (?x ?p ?y)] ]
     * [rdfs9: (?x rdfs:subClassOf ?y), (?a rdf:type ?x) -> (?a rdf:type ?y)]
     */
    final private void domain(X s, X p, X o, Output<X> out) {
        Set<X> x = setup.getDomain(p);
        x.forEach(c -> {
            derive(s, rdfType, c, out);
            subClass(s, rdfType, c, out);
            if ( setup.includeDerivedDataRDFS() )
                derive(p, rdfsDomain, c, out);
        });
    }

    /*
     * [rdfs3: (?p rdfs:range ?c) -> [(?y rdf:type ?c) <- (?x ?p ?y)] ]
     * [rdfs9: (?x rdfs:subClassOf ?y), (?a rdf:type ?x) -> (?a rdf:type ?y)]
     */
    final private void range(X s, X p, X o, Output<X> out) {
        // Range
        Set<X> x = setup.getRange(p);
        x.forEach(c -> {
            derive(o, rdfType, c, out);
            subClass(o, rdfType, c, out);
            if ( setup.includeDerivedDataRDFS() )
                derive(p, rdfsRange, c, out);
        });
    }
}

