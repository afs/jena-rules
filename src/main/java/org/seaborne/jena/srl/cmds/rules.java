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

package org.seaborne.jena.srl.cmds;

import java.util.Arrays;
import java.util.Optional;

import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.atlas.lib.Version;

public class rules {

    enum CMD { PARSE, EXECUTE , TRANSLATE }

    public static void main(String...args) {
        if ( args.length == 0 ) {
            System.err.println("Usage: rules SUB ARGS...");
            System.exit(1);
            //throw new CmdException("Usage: rules SUB ARGS...");
        }

        String cmd = args[0];
        String[] argsSub = Arrays.copyOfRange(args, 1, args.length);
        CMD cmdExec = null;
        String cmdUC = Lib.lowercase(cmd);

        // Help
        switch (cmdUC) {
            case "help" :
            case "-h" :
            case "-help" :
            case "--help" :
                System.err.println("Commands: execute (x), parse (p), translate(t)");
                return;
            case "version":
            case "--version":
            case "-version": {
                Optional<String> ver = Version.versionForClass(rules.class);
                Version.printVersion(System.err, "SPARQL-RL",  ver);
                System.exit(0);
            }
        }

        // Map to full name.
        cmdExec = switch (cmd) {
            case "exec", "execute", "x", "eval" -> CMD.EXECUTE;
            case "parse", "p", "print" -> CMD.PARSE;
            case "translate", "t", "sparql" -> CMD.TRANSLATE;
            default-> {
                System.err.println("Unknown: '"+cmd+"'");
                System.exit(1);
                yield null;
            }
        } ;

        // Execute sub-command
        switch (cmdExec) {
            case EXECUTE:         rules_eval.main(argsSub); break;
            case PARSE:           rules_parse.main(argsSub); break;
            case TRANSLATE:       rules_translate.main(argsSub); break;
            default: {
                System.err.println("Failed to find a command match for '"+cmd+"'");
                System.exit(1);
            }
        }
    }
}
