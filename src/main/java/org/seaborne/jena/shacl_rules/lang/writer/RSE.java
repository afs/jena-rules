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

package org.seaborne.jena.shacl_rules.lang.writer;

import org.apache.jena.sparql.sse.Tags;

public class RSE {
    public static final String tagRuleSet = "ruleset";
    public static final String tagData = "data";
    public static final String tagRule = "rule";

    public static final String tagHead = "head";
    public static final String tagBody = "body";

    public static final String tagFilter = "filter";
    public static final String tagSet = "set";
    public static final String tagNot = "not";

    public static final String tagTriple = Tags.tagTriple;
    public static final String tagSubject = Tags.tagSubject;
    public static final String tagProperty = Tags.tagPredicate;
    public static final String tagObject = Tags.tagObject;

    public static final String tagBase = "base";
    public static final String tagPrefixes = "prefixes";


}
