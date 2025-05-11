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

package org.seaborne.jena.shacl_rules.tuples0.store;

import java.util.Collection;
import java.util.List;

import org.apache.jena.atlas.iterator.Iter;
import org.seaborne.jena.shacl_rules.tuples0.rel.Rel;
import org.seaborne.jena.shacl_rules.tuples0.rel.RelStore;

/** Mutable {@link RelStore} */
public interface RelStoreAcc extends RelStore {
    public default RelStore freeze() {
        this.setReadOnly();
        return this;
    }

    public default void setReadOnly() { setWritable(false); }
    public void setWritable(boolean allowUpdate);

    public default boolean isReadonly() { return ! isUpdateable();}
    public boolean isUpdateable();

    public void add(Rel rel);

    public default void add(Collection<Rel> data) {
        data.stream().forEach(this::add);
    }

    public default void add(RelStore data) {
        data.stream().forEach(this::add);
    }

    public void delete(Rel rel);

    public void clear();

    public default void removeAll(Rel rel) {
        List<Rel> x = Iter.toList(find(rel));
        x.forEach((r) -> delete(r));
    }
}
