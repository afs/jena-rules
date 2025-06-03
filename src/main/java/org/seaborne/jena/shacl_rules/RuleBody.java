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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.syntax.*;

public class RuleBody {

    private final ElementGroup bodyGroup;
    private final Query bodyAsQuery;
    private final List<Triple> bodyTriples;

    public RuleBody(ElementGroup eltGroup) {
        this.bodyGroup = eltGroup;
        this.bodyAsQuery = elementToQuery(eltGroup);
        this.bodyTriples = elementToTriples(eltGroup);
    }
    public Query asQuery() {
        return bodyAsQuery;
    }

    public ElementGroup asElement() {
        return bodyGroup;
    }

    public List<Triple> getTriples() {
        return bodyTriples;
    }

    private static Query elementToQuery(ElementGroup eltGroup) {
        Query query = new Query();
        query.setQuerySelectType();
        query.setQueryResultStar(true);
        query.setQueryPattern(eltGroup);
        return query;
    }

    // Extract the triples from the body's Element Group.
    private static List<Triple> elementToTriples(ElementGroup eltGroup) {
        List<Triple> triples = new ArrayList<>();

        for ( Element e : eltGroup.getElements() ) {
            switch(e) {
                case ElementTriplesBlock tBlk -> {
                    triples.addAll(tBlk.getPattern().getList());
                }
                case ElementPathBlock pBlk -> {
                    BasicPattern bodyTriplePattern = new BasicPattern();
                    pBlk.getPattern().forEach(triplePath->{
                        // Better - sort out seq and alt.
                        Triple t = triplePath.asTriple();
                        if ( t == null )
                            throw new ShaclException("Path expression triples: "+triplePath);
                        triples.add(t);
                    });
                }
                case ElementFilter fBlk -> {/*ignore*/}
                default -> {
                    throw new ShaclException("Not supported for RDF output: "+e.getClass().getSimpleName());
                }
            }
        }
        return Collections.unmodifiableList(triples);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bodyAsQuery, bodyGroup);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( !(obj instanceof RuleBody) )
            return false;
        RuleBody other = (RuleBody)obj;
        return Objects.equals(bodyAsQuery, other.bodyAsQuery) && Objects.equals(bodyGroup, other.bodyGroup);
    }
}
