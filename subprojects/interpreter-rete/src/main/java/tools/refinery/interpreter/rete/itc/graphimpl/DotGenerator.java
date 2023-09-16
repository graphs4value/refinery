/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.graphimpl;

import tools.refinery.interpreter.rete.itc.alg.misc.scc.SCC;
import tools.refinery.interpreter.rete.itc.alg.misc.scc.SCCResult;
import tools.refinery.interpreter.matchers.util.IMemoryView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * This class contains utility methods to generate dot representations for {@link Graph} instances.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public class DotGenerator {

    private static final String[] colors = new String[] { "yellow", "blue", "red", "green", "gray", "cyan" };

    private DotGenerator() {

    }

    /**
     * Generates the dot representation for the given graph.
     *
     * @param graph
     *            the graph
     * @param colorSCCs
     *            specifies if the strongly connected components with size greater than shall be colored
     * @param nameFunction
     *            use this function to provide custom names to nodes, null if the default toString shall be used
     * @param colorFunction
     *            use this function to provide custom color to nodes, null if the default white color shall be used
     * @param edgeFunction
     *            use this function to provide custom edge labels, null if no edge label shall be printed
     * @return the dot representation as a string
     */
    public static <V> String generateDot(final Graph<V> graph, final boolean colorSCCs,
            final Function<V, String> nameFunction, final Function<V, String> colorFunction,
            final Function<V, Function<V, String>> edgeFunction) {
        final Map<V, String> colorMap = new HashMap<V, String>();

        if (colorSCCs) {
            final SCCResult<V> result = SCC.computeSCC(graph);
            final Set<Set<V>> sccs = result.getSccs();

            int i = 0;
            for (final Set<V> scc : sccs) {
                if (scc.size() > 1) {
                    for (final V node : scc) {
                        final String color = colorMap.get(node);
                        if (color == null) {
                            colorMap.put(node, colors[i % colors.length]);
                        } else {
                            colorMap.put(node, colorMap.get(node) + ":" + colors[i % colors.length]);
                        }
                    }
                    i++;
                }
            }

            // if a node has no color yet, then make it white
            for (final V node : graph.getAllNodes()) {
                if (!colorMap.containsKey(node)) {
                    colorMap.put(node, "white");
                }
            }
        } else {
            for (final V node : graph.getAllNodes()) {
                colorMap.put(node, "white");
            }
        }

        if (colorFunction != null) {
            for (final V node : graph.getAllNodes()) {
                colorMap.put(node, colorFunction.apply(node));
            }
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("digraph g {\n");

        for (final V node : graph.getAllNodes()) {
            final String nodePresentation = nameFunction == null ? node.toString() : nameFunction.apply(node);
            builder.append("\"" + nodePresentation + "\"");
            builder.append("[style=filled,fillcolor=" + colorMap.get(node) + "]");
            builder.append(";\n");
        }

        for (final V source : graph.getAllNodes()) {
            final IMemoryView<V> targets = graph.getTargetNodes(source);
            if (!targets.isEmpty()) {
                final String sourcePresentation = nameFunction == null ? source.toString() : nameFunction.apply(source);
                for (final V target : targets.distinctValues()) {
                    String edgeLabel = null;
                    if (edgeFunction != null) {
                        final Function<V, String> v1 = edgeFunction.apply(source);
                        if (v1 != null) {
                            edgeLabel = v1.apply(target);
                        }
                    }

                    final String targetPresentation = nameFunction == null ? target.toString()
                            : nameFunction.apply(target);

                    builder.append("\"" + sourcePresentation + "\" -> \"" + targetPresentation + "\"");
                    if (edgeLabel != null) {
                        builder.append("[label=\"" + edgeLabel + "\"]");
                    }
                    builder.append(";\n");
                }
            }
        }

        builder.append("}");
        return builder.toString();
    }

    /**
     * Generates the dot representation for the given graph. No special pretty printing customization will be applied.
     *
     * @param graph
     *            the graph
     * @return the dot representation as a string
     */
    public static <V> String generateDot(final Graph<V> graph) {
        return generateDot(graph, false, null, null, null);
    }

    /**
     * Returns a simple name shortener function that can be used in the graphviz visualization to help with readability.
     * WARNING: if you shorten the name of the {@link Node}s too much, the visualization may become incorrect because
     * grahpviz will treat different nodes as the same if their shortened names are the same.
     *
     * @param maxLength
     *            the maximum length of the text that is kept from the toString of the objects in the graph
     * @return the shrunk toString value
     */
    public static <V> Function<V, String> getNameShortener(final int maxLength) {
        return new Function<V, String>() {
            @Override
            public String apply(final V obj) {
                final String value = obj.toString();
                return value.substring(0, Math.min(value.length(), maxLength));
            }
        };
    }

}
