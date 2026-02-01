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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.riot.system.PrefixMap;
import org.seaborne.jena.shacl_rules.lang.writer.RuleSetWriter;

public class ShaclRulesWriter {

    public enum Style { Flat, MultiLine }


//    public static ShaclRulesWriter.Builder newBuilder() {
//        return new Builder();
//    }
//
//    public static class Builder {
//        Builder() {}
//
//        public ShaclRulesWriter.Builder syntax(ShaclRulesSyntax syntax)
//        { return this; }
//
//        public ShaclRulesWriter build() { return null; }
//    }

    public static void printBasic(RuleSet ruleSet) {
        ruleSet.getRules().forEach(r -> {
            write(System.out, r, ruleSet.getPrefixMap(), true);
        });
    }

    public static void print(RuleSet ruleSet) {
        write(System.out, ruleSet, true);
    }

    public static void print(RuleSet ruleSet, boolean flatMode) {
        write(System.out, ruleSet, flatMode);
    }

    public static void write(OutputStream outStream, RuleSet ruleSet, boolean flatMode) {
        Style style = flatMode ? Style.Flat : Style.MultiLine;
        IndentedWriter output = new IndentedWriter(outStream);
        RuleSetWriter.write(output, ruleSet, style);
    }

    /** Write a rule */
    public static void print(Rule rule) {
        write(System.out, rule, null, true);
    }

    /** Write a rule using a prefix map (not printed). */
    public static void print(Rule rule, PrefixMap prefixMap) {
        write(System.out, rule, prefixMap, true);
    }

    public static String asString(Rule rule, PrefixMap prefixMap) {
       try ( IndentedLineBuffer out = new IndentedLineBuffer() ) {
           write(out, rule, prefixMap, true);
           return out.asString();
       }
    }

    /** Write a rule (no prologue). */
    public static void write(OutputStream outStream, Rule rule, PrefixMap prefixMap, boolean flatMode) {
        IndentedWriter output = new IndentedWriter(outStream);
        try {
            write(output, rule, prefixMap, flatMode);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(IndentedWriter output, Rule rule, PrefixMap prefixMap, boolean flatMode) {
        Style style = flatMode ? Style.Flat : Style.MultiLine;
        RuleSetWriter.write(output, rule, prefixMap, null, style);
    }

    public static void write(IndentedWriter output, Rule rule, boolean flatMode) {
        Style style = flatMode ? Style.Flat : Style.MultiLine;
        RuleSetWriter.write(output, rule, null, null, style);
    }
}
