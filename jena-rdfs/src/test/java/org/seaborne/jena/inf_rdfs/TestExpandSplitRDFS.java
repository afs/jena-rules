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

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFOps;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.BeforeClass;
import org.seaborne.jena.inf_rdfs.setup.SetupRDFS_Node;

/** Test of RDFS.
 * Expanded graph, split vocab and data.
 */
public class TestExpandSplitRDFS extends AbstractTestGraphRDFS {

    private static Graph testGraphExpanded;
    @BeforeClass public static void setupHere() {
        testGraphExpanded = GraphFactory.createDefaultGraph();
        SetupRDFS_Node setup = InfFactory.setupRDF(vocab, false);
        StreamRDF stream = StreamRDFLib.graph(testGraphExpanded);
        stream = new InferenceStreamRDFS(stream, setup);
        StreamRDFOps.graphToStream(data, stream);
    }

    public TestExpandSplitRDFS() {
    }

    @Override
    protected boolean removeVocabFromReferenceResults() {
        return true;
    }

    @Override
    protected Graph getTestGraph() {
        return testGraphExpanded;
    }

    @Override
    protected String getTestLabel() {
        return "Expaned, combined";
    }
}

