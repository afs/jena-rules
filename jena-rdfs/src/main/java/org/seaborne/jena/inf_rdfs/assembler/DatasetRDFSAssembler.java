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

package org.seaborne.jena.inf_rdfs.assembler;

import static org.apache.jena.sparql.util.graph.GraphUtils.getAsStringValue;
import static org.apache.jena.sparql.util.graph.GraphUtils.getResourceValue;

import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.Mode;
import org.apache.jena.assembler.assemblers.AssemblerBase;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.seaborne.jena.inf_rdfs.DatasetGraphRDFS;
import org.seaborne.jena.inf_rdfs.InfFactory;
import org.seaborne.jena.inf_rdfs.SetupRDFS;

public class DatasetRDFSAssembler extends AssemblerBase implements Assembler {

    public static Resource getType() {
        return VocabRDFS.tDatasetRDFS;
    }

    @Override
    public Object open(Assembler a, Resource root, Mode mode) {
        DatasetGraph dsg = createDataset(a, root, mode) ;
        Dataset ds = DatasetFactory.wrap(dsg);
        return ds ;
    }

    /**
     * <pre>
     * &lt;#rdfsDS&gt; rdf:type ja:DatasetRDFS ;
     *      ja:rdfs "vocab.ttl";
     *      ja:dataset &lt;#baseDS&gt; ;
     *      .
     *
     * &lt;#baseDS&gt; rdf:type ja:MemoryDataset ;
     *     ja:data "data1.trig";
     *     ## ja:data "data2.trig";
     *     .
     * </pre>
     */

    public DatasetGraph createDataset(Assembler a, Resource root, Mode mode) {

        Resource dataset = getResourceValue(root, VocabRDFS.pDataset) ;
        if ( dataset == null )
            throw new AssemblerException(root, "Required base dataset missing: "+VocabRDFS.pDataset) ;

        Dataset base = (Dataset)Assembler.general.open(dataset);

//        String schemaFileString = getStringValue(root, pDataset) ;
//        String schemaFileURI = getStringValue(root, pDataset) ;

        String schemaFile = getAsStringValue(root, VocabRDFS.pRdfsSchemaFile);
        if ( schemaFile == null )
            throw new AssemblerException(root, "Required property missing: "+VocabRDFS.pRdfsSchemaFile) ;

        Graph schema = RDFDataMgr.loadGraph(schemaFile);
        SetupRDFS setup = InfFactory.setupRDFS(schema);
        DatasetGraph dsg = new DatasetGraphRDFS(base.asDatasetGraph(), setup);
        return dsg;
    }
}
