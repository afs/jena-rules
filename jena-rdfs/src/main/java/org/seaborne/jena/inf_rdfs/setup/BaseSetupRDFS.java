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
import java.util.*;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.riot.other.Transitive;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.tdb2.store.NodeId;
import org.seaborne.jena.inf_rdfs.engine.SetupRDFS;

/**
 * Core datastructures needed for RDFS.
 * To be general, this is in {@code <X>} space (e.g. {@link Node}, {@link NodeId}).
 */
public abstract class BaseSetupRDFS<X> implements SetupRDFS<X>{
    public final Graph vocabGraph;
    // Variants for with and without the key in the value side.

    private final Map<X, Set<X>> superClasses         = new HashMap<>();
    private final Map<X, Set<X>> superClassesInc      = new HashMap<>();
    private final Map<X, Set<X>> subClasses           = new HashMap<>();
    private final Map<X, Set<X>> subClassesInc        = new HashMap<>();
    private final Set<X> classes                      = new HashSet<>();
    private final Map<X, Set<X>> superPropertiesInc   = new HashMap<>();
    private final Map<X, Set<X>> superProperties      = new HashMap<>();
    private final Map<X, Set<X>> subPropertiesInc     = new HashMap<>();
    private final Map<X, Set<X>> subProperties        = new HashMap<>();
    // Predicate -> type
    private final Map<X, Set<X>> propertyRange        = new HashMap<>();
    private final Map<X, Set<X>> propertyDomain       = new HashMap<>();
    // Type -> predicate
    private final Map<X, Set<X>> rangeToProperty      = new HashMap<>();
    private final Map<X, Set<X>> domainToProperty     = new HashMap<>();
    // Whether we include the RDFS data in the results (as if TBox (rules) and ABox (ground data) are one unit).
    private final boolean includeDerivedDataRDFS$;
    private static String preamble = StrUtils.strjoinNL
        ("PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
         "PREFIX  rdfs:   <http://www.w3.org/2000/01/rdf-schema#>",
         "PREFIX  xsd:    <http://www.w3.org/2001/XMLSchema#>",
         "PREFIX  owl:    <http://www.w3.org/2002/07/owl#>",
         "PREFIX skos:    <http://www.w3.org/2004/02/skos/core#>");
//    protected BaseInfSetupRDFS(Graph vocab) {
//        this(vocab, false);
//    }

    protected BaseSetupRDFS(Graph vocab, boolean incDerivedDataRDFS) {
        includeDerivedDataRDFS$ = incDerivedDataRDFS;
        vocabGraph = vocab;

        // Find super classes - uses property paths
        exec("SELECT ?x ?y { ?x rdfs:subClassOf+ ?y }", vocab, superClasses, subClasses );
        //execTransitive(vocab, InfGlobal.rdfsSubClassOf, superClasses, subClasses);
        // Find properties
        exec("SELECT ?x ?y { ?x rdfs:subPropertyOf+ ?y }", vocab, superProperties, subProperties);
        //execTransitive(vocab, InfGlobal.rdfsSubPropertyOf, superProperties, subProperties );
        // Find domain
        exec("SELECT ?x ?y { ?x rdfs:domain ?y }", vocab, propertyDomain, domainToProperty);
        // Find range
        exec("SELECT ?x ?y { ?x rdfs:range ?y }", vocab, propertyRange, rangeToProperty);
        // All mentioned classes
        classes.addAll(superClasses.keySet());
        classes.addAll(subClasses.keySet());
        classes.addAll(rangeToProperty.keySet());
        classes.addAll(domainToProperty.keySet());
        deepCopyInto(superClassesInc, superClasses);
        addKeysToValues(superClassesInc);
        deepCopyInto(subClassesInc, subClasses);
        addKeysToValues(subClassesInc);
        deepCopyInto(superPropertiesInc, superProperties);
        addKeysToValues(superPropertiesInc);
        deepCopyInto(subPropertiesInc, subProperties);
        addKeysToValues(subPropertiesInc);
    }

    /** Calculate super/sub mapping */
    protected void execTransitive(Graph vocab, Node property, Map<X, Set<X>> superMap, Map<X, Set<X>> subMap) {
        Map<Node, Collection<Node>> map = Transitive.transitive(vocab, property);
        map.forEach((n,c)->{
            c.forEach(nc->{
                X a = fromNode(n);
                X b = fromNode(nc);
                put(superMap, a, b);
                put(subMap, b, a);
            });
        });
    }

    private void deepCopyInto(Map<X, Set<X>> dest, Map<X, Set<X>> src) {
        src.entrySet().forEach(e -> {
            Set<X> x = new HashSet<>(e.getValue());
            dest.put(e.getKey(), x);
        });
    }

    private void addKeysToValues(Map<X, Set<X>> map) {
        map.entrySet().forEach(e -> e.getValue().add(e.getKey()) );
    }

    private void exec(String qs, Graph graph, Map<X, Set<X>> multimap1, Map<X, Set<X>> multimap2) {
        Query query = QueryFactory.create(preamble + "\n" + qs, Syntax.syntaxARQ);
        QueryExecution qexec = QueryExecutionFactory.create(query, DatasetGraphFactory.wrap(graph));
        ResultSet rs = qexec.execSelect();
        for ( ; rs.hasNext() ; ) {
            Binding soln = rs.nextBinding();
            Node x = soln.get(Var.alloc("x"));
            Node y = soln.get(Var.alloc("y"));
            X a = fromNode(x);
            X b = fromNode(y);
            put(multimap1, a, b);
            put(multimap2, b, a);
        }
    }

    /** Go from Node space to X space for a node that is in the RDFS vocabulary.
     * This function is only passed Nodes that exist in the dataset.
     * Must not return null or "don't know".
     * @param node
     * @return
     */
    protected abstract X fromNode(Node node);
    @Override
    public boolean includeDerivedDataRDFS() {
        return includeDerivedDataRDFS$;
    }

    private static <X> void put(Map<X, Set<X>> multimap, X n1, X n2) {
        if ( !multimap.containsKey(n1) )
            multimap.put(n1, new HashSet<X>());
        multimap.get(n1).add(n2);
    }

    private Set<X> empty = Collections.emptySet();
    private Set<X> result(Map<X, Set<X>> map, X elt) {
        Set<X> x = map.get(elt);
        return x != null ? x : empty;
    }

    // get* : return the Set corresponding to element elt
    // get*Inc : return the Set corresponding to element elt incluinge self.

    @Override
    public Set<X> getSuperClasses(X elt) {
        return result(superClasses, elt);
    }

    @Override
    public Set<X> getSuperClassesInc(X elt) {
        return result(superClassesInc, elt);
    }

    @Override
    public Set<X> getSubClasses(X elt) {
        return result(subClasses, elt);
    }

    @Override
    public Set<X> getSubClassesInc(X elt) {
        return result(subClassesInc, elt);
    }

    @Override
    public Set<X> getSuperProperties(X elt) {
        return result(superProperties, elt);
    }

    @Override
    public Set<X> getSuperPropertiesInc(X elt) {
        return result(superPropertiesInc, elt);
    }

    @Override
    public Set<X> getSubProperties(X elt) {
        return result(subProperties, elt);
    }

    @Override
    public Set<X> getSubPropertiesInc(X elt) {
        return result(subPropertiesInc, elt);
    }

    @Override
    public boolean hasRangeDeclarations() {
        return ! propertyRange.isEmpty();
    }

    @Override
    public boolean hasDomainDeclarations() {
        return ! propertyDomain.isEmpty();
    }

    @Override
    public Set<X> getRange(X elt) {
        return result(propertyRange, elt);
    }

    @Override
    public Set<X> getDomain(X elt) {
        return result(propertyDomain, elt);
    }

    @Override
    public Set<X> getPropertiesByRange(X elt) {
        return result(rangeToProperty, elt);
    }

    @Override
    public Set<X> getPropertiesByDomain(X elt) {
        return result(domainToProperty, elt);
    }
}
