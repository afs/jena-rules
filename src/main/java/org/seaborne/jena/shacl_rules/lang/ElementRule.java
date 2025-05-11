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

package org.seaborne.jena.shacl_rules.lang;

import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.TripleCollector;
import org.apache.jena.sparql.syntax.TripleCollectorBGP;

/**
 * Parser structure.
 */
public class ElementRule {
    // May be unnecessary and instead directly create the Rule object

    private final BasicPattern head;
    private final ElementGroup body;

    public ElementRule(TripleCollector head, ElementGroup body) {
        TripleCollectorBGP tcBGP = (TripleCollectorBGP)head;
        this.head = tcBGP.getBGP();
        this.body = body;
    }

    public BasicPattern getHead() {
        return head;
    }

    public ElementGroup getBody() {
        return body;
    }

    @Override
    public String toString() {
        String x = body.toString();
        x = x.replace("\n", " ");
        x = x.replaceAll("  +", " ");
        return head.toString() + " :- " + x;
    }
}
