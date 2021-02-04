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

package org.seaborne.jena.inf_rdfs.setup;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
/** RDFS setup in Node space */
public class SetupRDFS_Node extends BaseSetupRDFS<Node> {

    /** {@code incDerivedDataRDFS} causes the engine to look for RDFS relationships in the data
     * as if TBox (rules) and ABox (ground data) are one unit.
     * Set true if abox == tbox.
     * Can choose false or true if abox != tbox.
     */
    public SetupRDFS_Node(Graph vocab, boolean incDerivedDataRDFS) {
        super(vocab, incDerivedDataRDFS);
    }

    @Override
    protected Node fromNode(Node node) {
        return node;
    }
}
