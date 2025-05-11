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

import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.syntax.ElementGroup;

public class Rule {

    private final BasicPattern head; // XXX Template
    private final ElementGroup body; // XXX temp
    private final Query bodyAsQuery; // XXX temp

    public Rule(List<Triple> triples, ElementGroup body) {
        this.head = BasicPattern.wrap(triples);
        this.body = body;
        this.bodyAsQuery = asQuery(body);   // Compute once.
    }

    public BasicPattern getHead() {
        return head;
    }

    // Currently, the parser structure.
    public ElementGroup getBody() {
        return body;
    }

    public Query bodyAsQuery() {
        return bodyAsQuery;
    }

    private static Query asQuery(ElementGroup eltGroup) {
        Query query = new Query();
        query.setQuerySelectType();
        query.setQueryResultStar(true);
        query.setQueryPattern(eltGroup);
        return query;
    }

    @Override
    public String toString() {
        String x = body.toString();
        x = x.replace("\n", " ");
        x = x.replaceAll("  +", " ");
        return head.toString() + " :- " + x;
    }

}
