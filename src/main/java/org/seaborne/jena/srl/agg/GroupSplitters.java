/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 */

package org.seaborne.jena.srl.agg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

/**
 * Library of splitters.
 */
public class GroupSplitters {

    private GroupSplitters() {}

    // COUNT(*) for the whole group.
    private static final GroupSplitter instanceStar = new GroupSplitterStar();



    static GroupSplitter splitterStar() { return instanceStar; }
    static GroupSplitter splitterVars(Collection<Var> groupVars) { return new GroupSplitterByVars(groupVars); }

    /* * or vars */
    static GroupSplitter splitter(Collection<Var> groupVars) {
        if ( groupVars == null || groupVars.isEmpty() )
            return instanceStar;
        return splitterVars(groupVars);
    }

    private static class GroupSplitterStar implements GroupSplitter {

        GroupSplitterStar() {}

        @Override
        public GroupKey groupKey(Binding binding) { return GroupKeyFixed.instance; }

        @Override
        public Set<Var> groupKeys() { return Set.of(); }
    }


    private static class GroupSplitterByVars implements GroupSplitter {

        private final Collection<Var> groupVars;

        GroupSplitterByVars(Collection<Var> groupVars) {
            this.groupVars = groupVars;
        }

        @Override
        public GroupKey groupKey(Binding binding) {
            List<Var> keyVars = List.copyOf(groupVars);
            List<Node> key = new ArrayList<>(groupVars.size());
            for ( Var var : groupVars ) {
                Node n = binding.get(var);
                if ( n == null )
                    throw new InternalErrorException("GroupSplitterByVars - no binding for "+var);
                key.add(n);
            }
            return new GroupKeyList(keyVars, key);
        }

        @Override
        public Collection<Var> groupKeys() {
            return groupVars;
        }
    };



}
