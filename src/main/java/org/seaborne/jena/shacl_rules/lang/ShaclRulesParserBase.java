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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.lang.SPARQLParserBase;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.TripleCollector;
import org.apache.jena.sparql.syntax.TripleCollectorBGP;

public class ShaclRulesParserBase extends SPARQLParserBase {

    private List<ElementRule> rules = new ArrayList<>();
    private List<Triple> data = new ArrayList<>();

    public void startRules() {}
    public void finishRules() {}

    //private ElementRule currentShaclRule;

    public void startRule() {}
    public void finishRule(TripleCollector head, ElementGroup body, int line, int column) {
        ElementRule rule = new ElementRule(head, body);
        rules.add(rule);
    }

    public List<ElementRule> getRules() { return rules; }

    public void startData() {}
    public void finishData(TripleCollectorBGP triples, int line, int column) {
        triples.getBGP().getList().forEach(triple->{
            if ( ! triple.isConcrete() )
                throw new ShaclParseException("Triple contains variables", line, column);
            data.add(triple);
        });
    }

    // Gather into a ElementGroup

    public void emit(Triple triple) {}
    public void emit(Expr expression) {}

    public List<Triple> getData() { return data; }
}
