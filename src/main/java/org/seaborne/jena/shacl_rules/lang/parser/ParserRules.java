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

package org.seaborne.jena.shacl_rules.lang.parser;

import static org.apache.jena.riot.SysRIOT.fmtMessage;

import org.apache.jena.riot.system.ErrorHandler;
import org.slf4j.Logger;

public class ParserRules {

    /** Messages to a logger. Adds line/column information. This is not an ErrorHandler */
    protected static class ErrorLogger {
        protected final Logger log ;

        public ErrorLogger(Logger log) {
            this.log = log ;
        }

        /** report a warning */
        public void logWarning(String message, long line, long col) {
            if ( log != null )
                log.warn(fmtMessage(message, line, col)) ;
        }

        /** report an error */
        public void logError(String message, long line, long col) {
            if ( log != null )
                log.error(fmtMessage(message, line, col)) ;
        }

        /** report a catastrophic error */
        public void logFatal(String message, long line, long col) {
            if ( log != null )
                logError(message, line, col) ;
        }
    }

    protected static class ErrorHandlerRuleParser extends ErrorLogger implements ErrorHandler {

        public ErrorHandlerRuleParser(Logger log) {
            super(log);
        }

        @Override
        public void warning(String message, long line, long col) {
            logWarning(message, line, col);
        }

        @Override
        public void error(String message, long line, long col) {
            logError(message, line, col) ;
            throw new ShaclRulesParseException(message, (int)line, (int)col);
        }

        @Override
        public void fatal(String message, long line, long col) {
            logFatal(message, line, col) ;
            throw new ShaclRulesParseException(message, (int)line, (int)col);
        }
    }
}
