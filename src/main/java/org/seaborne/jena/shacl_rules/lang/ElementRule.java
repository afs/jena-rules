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

import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.syntax.*;

/**
 * Parser structure.
 */
public class ElementRule {
    // May be unnecessary and instead directly create the Rule object

    private final BasicPattern head;
    private final ElementGroup body;

    public ElementRule(BasicPattern bgp, ElementGroup body) {
        this.head = bgp;
        this.body = fixup(body);
    }

    // Convert ElementPathBlocks to ElementTripleBlocks.
    // XXX This should go away.
    private static ElementGroup fixup(ElementGroup eltGroup) {
        ElementGroup newEltGroup = new ElementGroup();
        List<Element> elts = eltGroup.getElements();
        for ( int i = 0 ; i < elts.size() ; i++ ) {
            Element elt = elts.get(i);

            switch(elt) {
                case ElementPathBlock epb -> {
                    ElementTriplesBlock etb = new ElementTriplesBlock();
                    epb.getPattern().getList().forEach(triplePath->{
                        Triple t = triplePath.asTriple();
                        if ( t == null )
                            throw new ShaclException("Path: "+triplePath);
                        etb.addTriple(t);
                    });
                    newEltGroup.addElement(etb);
                    continue;
                }
                // Pass through.
                case ElementTriplesBlock x -> {}
                case ElementFilter x -> {}
                case ElementBind x -> {}
                case ElementAssign x -> {}
                default -> {
                    throw new ShaclException("Unexpected element: "+elt.getClass().getSimpleName());
                }
            }
            newEltGroup.addElement(elt);
        }
        return newEltGroup;
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
