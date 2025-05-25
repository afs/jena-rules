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

import java.util.Objects;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.syntax.ElementGroup;

public class RuleBody {

    private final ElementGroup bodyGroup;
    private final Query bodyAsQuery;

    public RuleBody(ElementGroup eltGroup) {
        this.bodyGroup = eltGroup;
        this.bodyAsQuery = elementToQuery(eltGroup);
    }

    public Query asQuery() {
        return bodyAsQuery;
    }

    public ElementGroup asElement() {
        return bodyGroup;
    }

    private static Query elementToQuery(ElementGroup eltGroup) {
        Query query = new Query();
        query.setQuerySelectType();
        query.setQueryResultStar(true);
        query.setQueryPattern(eltGroup);
        return query;
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
