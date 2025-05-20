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

package org.seaborne.jena.shacl_rules.rdf_syntax.expr;

import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.ShaclException;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.sparql.util.graph.GNode;
import org.apache.jena.sparql.util.graph.GraphList;
import org.apache.jena.system.G;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.jena.shacl_rules.rdf_syntax.V;
import org.seaborne.jena.shacl_rules.rdf_syntax.expr.FunctionEverything.Build;

public class RdfToExpr {

    //SparqlNodeExpression.fromRDF
    public static Expr buildExpr(Map<String, Build> mapBuild, Graph graph) {
        /// Top level.
        Node topExpression = G.getOnePO(graph, RDF.Nodes.type, V.sparqlExprClass);
        // Many?

        Expr expr;
        try {
            Node expression = G.getZeroOrOneSP(graph, topExpression, V.expr);
            if ( expression != null )
                expr = buildExpr(mapBuild, graph, expression);
            else
                expr = buildSparqlExpr(graph, topExpression);
            if ( expr == null)
                throw new ShaclException("sh:expr not found (nor sh:sparqlExpr)");
            return expr;
        } catch (Exception ex) {
            System.out.println("** failed to rebuild expr: "+ex.getMessage());
            ex.printStackTrace();
            return null;
        }

    }

    //SparqlNodeExpression.rdfToSparqlExpr
    static Expr buildExpr(Map<String, Build> mapBuild, Graph graph, Node x) {
        if ( ! x.isBlank() )
            return NodeValue.makeNode(x);

        //private static Node extractVar(Graph graph, Node node) {
            Node vx = G.getZeroOrOneSP(graph, x, V.var);
            if ( vx != null ) {
                //Var v = Var.alloc(G.asString(vx));
                return new ExprVar(G.asString(vx));
            }
        //}

        Triple t = G.find(graph, x, null, null)
                // Ignores type,
                //.filterDrop(tt->tt.getPredicate().equals(RDF.Nodes.type))
                .next();

        Node pFunction = t.getPredicate();
        Node o = t.getObject();

        GNode gn = new GNode(graph, o);
        List<Node> list = GraphList.members(gn);

        // Replaces:
        //List<Expr> args = list.stream().map(n-> SparqlNodeExpression.rdfToSparqlExpr(graph, n)).toList();
        List<Expr> args = list.stream().map(n-> buildExpr(mapBuild, graph, n)).toList();

        String functionURI = pFunction.getURI();
        Build build = mapBuild.get(pFunction.getURI());
        if ( build == null )
            throw new RuntimeException("Build: "+functionURI);

        // ???
        Expr[] array = args.toArray(Expr[]::new);
        Expr expr = build.build(functionURI, array);
        return expr;
    }

    static Expr buildSparqlExpr(Graph graph, Node root) {
        Node exprSparqlNode = G.getZeroOrOneSP(graph, root, V.sparqlExpr);
        if ( exprSparqlNode == null )
            return null;
        if ( ! G.isString(exprSparqlNode) )
            throw new ShaclException("Not a simple string: "+exprSparqlNode);
        String exprString = G.asString(exprSparqlNode);
        return ExprUtils.parse(exprString);
    }



}
