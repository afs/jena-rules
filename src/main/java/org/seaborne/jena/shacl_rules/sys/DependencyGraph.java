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

package org.seaborne.jena.shacl_rules.sys;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.seaborne.jena.shacl_rules.Rule;
import org.seaborne.jena.shacl_rules.RuleSet;
import org.seaborne.jena.shacl_rules.exec.RuleOps;
import org.seaborne.jena.shacl_rules.writer.ShaclRulesWriter;

/**
 * Rules dependency graph. The graph has vertices of rules and links being "depends
 * on" another rule, i.e. for a triple that is in the head of a rule, it is the rules
 * that can generate that triple. relations in its body. from this, we can determine
 * whether a rule is:
 * <ul>
 * <li>Data only (body contains relations that only appear in the data)</li>
 * <li>Not-recursive: it can be solved by top-down flattening</li>
 * <li>Mutually recursive rules</li>
 * <li>linear (only one body relationship is recursive)</li>
 * </ul>
 */

public class DependencyGraph {

   // Rule -> other rules it depends on
   private MultiValuedMap<Rule, Rule> direct = MultiMapUtils.newListValuedHashMap();

   // Rule without dependent rules (the rule is satisfied by the data directly)
   private Set<Rule> level0 = new HashSet<>();

   // XXX Necessary?
   // Body triple to rules that can generate that triple
   private MultiValuedMap<Triple, Rule> providers = MultiMapUtils.newListValuedHashMap();

   private final RuleSet ruleSet;

   public static DependencyGraph create(RuleSet ruleSet) {
       DependencyGraph depGraph = new DependencyGraph(ruleSet);
       //depGraph.initialize();
       return depGraph;
   }

   private DependencyGraph(RuleSet ruleSet) {
       this.ruleSet = ruleSet;
       initialize();
   }

   private void initialize() {
       ruleSet.getRules().forEach(r->{
          this.put(r);
       });
   }

   public RuleSet getRuleSet() {
       return ruleSet;
   }

   private boolean DEBUG_BUILD = false;

   private void put(Rule rule) {
       Collection<Rule> c = RuleOps.dependencies(rule, ruleSet);
       if ( DEBUG_BUILD )
           System.out.println(c.size()+" :: put:"+rule);
       if ( c.isEmpty() ) {
           level0.add(rule);
       } else {
           direct.putAll(rule, c);
       }

       rule.getBody().getTriples().forEach(triple->{
           // Look for rules with this triple (or a generalization) in the head
           ruleSet.getRules().forEach(r->{
               if ( RuleOps.dependsOn(triple,  rule) ) {
                   providers.put(triple,rule);
               }
           });
       });
   }

   public void walk(Rule rule, Consumer<Rule> action) {
       walk$(rule, action);
   }

   // Use the direct set.

   private void walk$(Rule rule, Consumer<Rule> action) {
       Set<Rule> acc = new HashSet<>();
       Deque<Rule> stack = new ArrayDeque<>();
       walk$(rule, action, acc, stack);
   }

   private void walk$(Rule rule, Consumer<Rule> action, Set<Rule> visited, Deque<Rule> pathVisited) {
       if ( visited.contains(rule) )
           return;
       visited.add(rule);
       action.accept(rule);
       pathVisited.push(rule);
       walkStep(rule, action, visited, pathVisited);
       pathVisited.pop();
   }

   private void walkStep(Rule rule, Consumer<Rule> action, Set<Rule> visited, Deque<Rule> pathVisited) {
       Collection<Rule> others = direct.get(rule);
       for ( Rule otherRule : others ) {
           walk$(otherRule, action, visited, pathVisited);
       }
   }

   private final static boolean DEBUG_RECURSIVE = false;

   // Recursion test. Like walk but with early exit.
   // Can terminate early if all we want is whether it is/is not recursive.
   public boolean isRecursive(Rule rule) {
       Deque<Rule> stack = new ArrayDeque<>();
       boolean b = isRecursive(rule, rule, stack);
       if ( b ) {
           if ( DEBUG_RECURSIVE ) {
               stack.stream().map(r->r.getHead()).forEach(h->System.out.printf("--%s", h));
               System.out.println();
               System.out.println(stack);
           }
       }
       return b;
   }

   // XXX "matches" - considers variables.

   private boolean isRecursive(Rule topRule, Rule rule, Deque<Rule> visited) {
       if ( DEBUG_RECURSIVE )
           System.out.printf("isRecursive(\n  %s,\n  %s,\n  %s)\n", topRule, rule, visited);
       if ( ! visited.isEmpty() && topRule.equals(rule))
           return true;
       if ( visited.contains(rule) )
           // Other cycle.
           return false;
       visited.push(rule);
       boolean b = isRecursive2(topRule, rule, visited) ;
       if ( b )
           return b;
       visited.pop();
       return false;
   }

   // topRule is the overall rule we are testing. */
   private boolean isRecursive2(Rule topRule, Rule visitRule, Deque<Rule> visited) {
       Collection<Rule> providedBy = direct.get(visitRule);
       for( Rule otherRule : providedBy ) {
           if ( isRecursive(topRule, otherRule, visited) )
               return true;
       }
       return false;
   }

   public void print() { print(IndentedWriter.stdout); }

   public void print(PrintStream pStream) {
       print(new IndentedWriter(pStream));
   }

   public void print(IndentedWriter out) {
       try ( out ) {
           out.println("[DependencyGraph]");
           out.incIndent();
           for ( Rule r : direct.keySet() ) {
               ShaclRulesWriter.print(out, r, ruleSet.getPrefixMap(), true);
               Collection<Rule> c = direct.get(r);
               c.forEach(rr -> {
                   out.incIndent(4);
                   ShaclRulesWriter.print(out, rr, ruleSet.getPrefixMap(), true);
                   out.decIndent(4);
               });
           }
           out.decIndent();
       }
   }
}
