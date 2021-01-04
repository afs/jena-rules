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

package org.seaborne.jena.rules.store;

import java.util.Collection;

import org.seaborne.jena.rules.Rel;
import org.seaborne.jena.rules.RelStore;

public interface RelStoreBuilder {

    public default RelStoreBuilder add(RelStore relStore) {
        relStore.all().forEach(this::add);
        return this;
    }

    public default RelStoreBuilder add(Collection<Rel> rels) {
        rels.forEach(r->this.add(r));
        return this;
    }

    public RelStoreBuilder add(Rel rel);

    public RelStoreBuilder delete(Rel rel);

    public RelStore build();

}