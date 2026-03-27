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

package org.seaborne.jena.shacl_rules.sys;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotNotFoundException;
import org.apache.jena.riot.system.streammgr.StreamManager;
import org.apache.jena.riot.web.HttpNames;

public class ImportsLib {

    // From RDFparser - make jena wide.
    // Builderize.
    static TypedInputStream openTypedInputStream(HttpClient httpClient,
                                                 String urlStr, Path path,
                                                 StreamManager streamManager,
                                                 String acceptHeader, Map<String, String> httpHeaders) {
        // If path, use that.
        if ( path != null ) {
            try {
                InputStream in = Files.newInputStream(path);
                ContentType ct = RDFLanguages.guessContentType(urlStr) ;
                return new TypedInputStream(in, ct);
            }
            catch (NoSuchFileException | FileNotFoundException ex)
            { throw new RiotNotFoundException() ;}
            catch (IOException ex) { IO.exception(ex); }
        }

        TypedInputStream in;
        // Need more control than LocatorURL provides to get the Accept header
        // in and the HttpCLient setup.
        // So map now.
        urlStr = streamManager.mapURI(urlStr);
        if ( urlStr.startsWith("http://") || urlStr.startsWith("https://") ) {
            // HTTP
            //String acceptHeader = HttpLib.dft(appAcceptHeader, WebContent.defaultRDFAcceptHeader);
            HttpRequest request = HttpLib.newGetRequest(urlStr, (b)->{
                if ( httpHeaders != null )
                    httpHeaders.forEach(b::header);
                b.setHeader(HttpNames.hAccept, acceptHeader);
            });
            // Setup of the HTTP client, if not provided by RDFParserBuilder
            final var httpClientToUse = ( httpClient != null ) ? httpClient : HttpEnv.getHttpClient(urlStr);
            HttpResponse<InputStream> response = HttpLib.execute(httpClientToUse, request);
            in = HttpLib.handleResponseTypedInputStream(response);
        } else {
            // Already mapped.
            in = streamManager.openNoMapOrNull(urlStr);
        }
        if ( in == null )
            throw new RiotNotFoundException("Not found: "+urlStr);
        return in ;
    }
}
