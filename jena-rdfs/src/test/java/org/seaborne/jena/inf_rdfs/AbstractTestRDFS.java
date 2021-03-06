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

package org.seaborne.jena.inf_rdfs;

import static org.seaborne.jena.inf_rdfs.engine.InfGlobal.rdfType;
import static org.seaborne.jena.inf_rdfs.engine.InfGlobal.rdfsSubClassOf;
import static org.seaborne.jena.inf_rdfs.engine.InfGlobal.rdfsSubPropertyOf;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.FileUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.seaborne.jena.inf_rdfs.engine.InfGlobal;

/** Testing based on a graph under test ({@link #getTestGraph()}) and a reference graph
 * ({@link #getReferenceGraph()}) that is assumed to return the correct answers.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractTestRDFS {
    private static PrintStream      out = System.err;
    static Node node(String str) { return NodeFactory.createURI("http://example/"+str) ; }

    @Test public void test_rdfs_01()        { test(node("a"), rdfType, null) ; }
    @Test public void test_rdfs_02()        { test(node("a"), rdfType, node("T2")) ; }
    @Test public void test_rdfs_03()        { test(null, rdfType, node("T2")) ; }

    @Test public void test_rdfs_04()        { test(null, null, node("T2")) ; }
    @Test public void test_rdfs_05()        { test(null, rdfType, node("T")) ; }
    @Test public void test_rdfs_05a()       { test(null, null, node("T")) ; }

    @Test public void test_rdfs_06()        { test(node("c"), rdfType, null) ; }
    @Test public void test_rdfs_06a()       { test(node("c"), null, null) ; }

    @Test public void test_rdfs_07()        { test(null, rdfType, null) ; }
    @Test public void test_rdfs_08()        { test(null, node("q"), null) ; }

    @Test public void test_rdfs_08a()       { test(null, node("p"), null) ; }
    @Test public void test_rdfs_08b()       { test(null, node("pp"), null) ; }
    @Test public void test_rdfs_08c()       { test(null, node("ppp"), null) ; }
    @Test public void test_rdfs_08d()       { test(null, node("pTop"), null) ; }

    @Test public void test_rdfs_09()        { test(node("z"), null, null) ;  }
    @Test public void test_rdfs_10()        { test(node("z"), rdfType, null) ; }

    @Test public void test_rdfs_12a()       { test(null, rdfType, node("P")) ; }
    @Test public void test_rdfs_12b()       { test(null, rdfType, node("P1")) ; }
    @Test public void test_rdfs_12c()       { test(null, rdfType, node("P2")) ; }
    @Test public void test_rdfs_12d()       { test(null, null, node("P")) ; }
    @Test public void test_rdfs_12e()       { test(null, null, node("P1")) ; }
    @Test public void test_rdfs_12f()       { test(null, null, node("P2")) ; }

    @Test public void test_rdfs_13a()       { test(null, rdfType, node("Q")) ; }
    @Test public void test_rdfs_13b()       { test(null, rdfType, node("Q1")) ; }
    @Test public void test_rdfs_13c()       { test(null, rdfType, node("Q2")) ; }
    @Test public void test_rdfs_13d()       { test(null, null, node("Q")) ; }
    @Test public void test_rdfs_13e()       { test(null, null, node("Q1")) ; }
    @Test public void test_rdfs_13f()       { test(null, null, node("Q2")) ; }

    // all T cases.
    // all U cases.
    @Test public void test_rdfs_14a()       { test(null, rdfType, node("T")) ; }
    @Test public void test_rdfs_14b()       { test(null, rdfType, node("T1")) ; }
    @Test public void test_rdfs_14c()       { test(null, rdfType, node("S2")) ; }
    @Test public void test_rdfs_14d()       { test(null, null, node("T")) ; }
    @Test public void test_rdfs_14e()       { test(null, null, node("T1")) ; }
    @Test public void test_rdfs_14f()       { test(null, null, node("S2")) ; }

    @Test public void test_rdfs_15a()       { test(null, rdfType, node("U")) ; }
    @Test public void test_rdfs_15b()       { test(null, null, node("U")) ; }

    // Not in data, not vocab.
    @Test public void test_rdfs_16a()       { test(null, null, node("Other")) ; }
    @Test public void test_rdfs_16b()       { test(null, rdfType, node("Other")) ; }

    // In data, not vocab.
    @Test public void test_rdfs_17a()       { test(null, null, node("X")) ; }
    @Test public void test_rdfs_17b()       { test(null, rdfType, node("X")) ; }

    @Test public void test_rdfs_20()        { test(null, node("p"), null) ; }
    @Test public void test_rdfs_21()        { test(null, node("pp"), null) ; }
    @Test public void test_rdfs_22()        { test(null, node("ppp"), null) ; }
    @Test public void test_rdfs_23()        { test(null, node("pTop"), null) ; }



    @Test public void test_rdfs_30()        { test(node("e"), null, null) ; }
    @Test public void test_rdfs_31()        { test(node("e"), node("r"), null) ; }

    // [RDFS] Renumber
    @Test public void test_rdfs_40()        { test(null, rdfsSubClassOf, null); }
    @Test public void test_rdfs_40a()       { test(node("T3"), rdfsSubClassOf, null); }
    @Test public void test_rdfs_40b()       { test(null, rdfsSubClassOf, node("T3")); }
    @Test public void test_rdfs_40c()       { test(node("T3"), rdfsSubClassOf, node("T3")); }
    @Test public void test_rdfs_40c2()       { test(node("T3"), rdfsSubClassOf, node("U")); }

    @Test public void test_rdfs_41a()       { test(node("T"), rdfsSubClassOf, null); }
    @Test public void test_rdfs_41b()       { test(null, rdfsSubClassOf, node("T")); }
    @Test public void test_rdfs_41c()       { test(node("T"), rdfsSubClassOf, node("T")); }
    @Test public void test_rdfs_41c2()       { test(node("T"), rdfsSubClassOf, node("U")); }

    @Test public void test_rdfs_42a()       { test(node("U"), rdfsSubClassOf, null); }
    @Test public void test_rdfs_42b()       { test(null, rdfsSubClassOf, node("U")); }
    @Test public void test_rdfs_42c()       { test(node("U"), rdfsSubClassOf, node("U")); }


    // [RDFS] Dupl to 41
    @Test public void test_rdfs_40d()       { test(node("NO"), rdfsSubClassOf, null); }
    @Test public void test_rdfs_40e()       { test(null, rdfsSubClassOf, node("NO")); }
    @Test public void test_rdfs_40f()       { test(node("NO"), rdfsSubClassOf, node("NO")); }
    @Test public void test_rdfs_40g()       { test(node("NO"), rdfsSubClassOf, node("U")); }
    @Test public void test_rdfs_40h()       { test(node("T3"), rdfsSubClassOf, node("T")); }    // No.

    @Test public void test_rdfs_50()        { test(null, rdfsSubPropertyOf, null); }
    @Test public void test_rdfs_50a()       { test(node("p"), rdfsSubPropertyOf, null); }
    @Test public void test_rdfs_50b()       { test(null, rdfsSubPropertyOf, node("p")); }
    @Test public void test_rdfs_50c()       { test(node("p"), rdfsSubPropertyOf, node("p")); }

    @Test public void test_rdfs_99_all()    { test(null, null, null) ; }



    protected void test(Node s, Node p, Node o) {
        //RDFDataMgr.write(System.out, getReferenceGraph(), RDFFormat.TURTLE_FLAT);
        // The "right" answers (generated by Jena Inference Engine with rdfs-min.rules).
        List<Triple> expected = findInGraph(getReferenceGraph(), s, p, o);

        if ( removeVocabFromReferenceResults() )
            expected = InfGlobal.removeRDFS(expected);

        // Graph under test.
        List<Triple> actual = findInGraph(getTestGraph(), s, p, o);

        boolean b = ListUtils.equalsUnordered(expected, actual);
        if ( ! b ) {
            out.println("Fail: find("+s+", "+p+", "+o+")");
            printDiff(out, expected, actual);
        }

        Assert.assertTrue(getTestLabel(), b);
    }

    protected List<Triple> findInGraph(Graph graph, Node s, Node p, Node o) {
        return graph.find(s,p,o).toList();
    }

    protected static Graph createRDFSGraph(Model data, Model vocab) {
        return ModelFactory.createRDFSModel(vocab, data).getGraph();
    }

    /** Create a Jena-rules backed graph */
    protected static Graph createRulesGraph(Model data, Model vocab, String rulesFile) {
        try {
            String rules = FileUtils.readWholeFileAsUTF8(rulesFile);
            rules = rules.replaceAll("#[^\\n]*", "");
            Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
            // Model m = ModelFactory.createInfModel(reasoner, data);
            /** Rules way */
            InfModel m = ModelFactory.createInfModel(reasoner, vocab, data);
            return m.getGraph();
        }
        catch (IOException ex) { IO.exception(ex) ; return null ; }
    }

    /** Indicate whether the vocabulary is visible in the answers */
    protected abstract boolean removeVocabFromReferenceResults();

    /** Return the graph that gives the right answers */
    protected abstract Graph getReferenceGraph();

    /** Return the graph under test */
    protected abstract Graph getTestGraph();

    /** Return a label for the reference graph */
    protected abstract String getReferenceLabel();

    /** Return a label for the graph under test */
    protected abstract String getTestLabel();

    //find("+s+", "+p+", "+o+")"
    static protected <X> void printDiff(PrintStream out, List<X> expected, List<X> actual) {
        if ( actual.size() < 10 ) {
            out.println("Actual:");
            if ( actual.isEmpty() )
                out.println("  Empty");
            else
                actual.forEach(t->out.println("  "+t));
        }

        Pair<List<X>, List<X>> diff = ListUtils.listDiffBoth(expected, actual);
        out.println("  Diff expected\\actual");
        diff.getLeft().forEach(t->out.println("    Expected, not actual: "+t));
        out.println("  Diff actual\\expected:");
        diff.getRight().forEach(t->out.println("   Actual, not expected: "+t));
        out.println();

////        A.stream().forEach(item -> out.println("1: "+item));
////        B.stream().forEach(item -> out.println("2: "+item));
//        Set<X> aa = new HashSet<>(A);
//        Set<X> bb = new HashSet<>(B);
//        SetUtils.difference(aa, bb).stream().forEach(item -> out.println("> "+item));
//        SetUtils.difference(bb, aa).stream().forEach(item -> out.println("< "+item));
//        out.println();
    }

    static List<Triple> print(PrintStream out, List<Triple> x) {
        return print(out, "  ", x);
    }

    static List<Triple> print(PrintStream out, String leader, List<Triple> x) {
        List<Triple> list = Iter.toList(x.iterator());

        if ( list.isEmpty() )
            out.println(leader+"<empty>");
        else
            list.stream().forEach(triple -> {out.println(leader+triple) ; });
        return list;
    }
}

