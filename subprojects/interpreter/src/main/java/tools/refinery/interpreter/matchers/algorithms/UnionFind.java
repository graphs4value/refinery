/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.algorithms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.util.CollectionsFactory;

/**
 * Union-find data structure implementation. Note that the implementation relies on the correct implementation of the
 * equals method of the type parameter's class.
 *
 * @author Tamas Szabo
 *
 * @param <V>
 *            the type parameter of the element's stored in the union-find data structure
 */
public class UnionFind<V> {

    private final Map<V, UnionFindNodeProperty<V>> nodeMap;
    final Map<V, Set<V>> setMap;

    /**
     * Instantiate a new union-find data structure.
     */
    public UnionFind() {
        nodeMap = CollectionsFactory.createMap();
        setMap = CollectionsFactory.createMap();
    }

    /**
     * Instantiate a new union-find data structure with the given elements as separate sets.
     */
    public UnionFind(Iterable<V> elements) {
        this();
        for (V element : elements) {
            makeSet(element);
        }
    }

    /**
     * Creates a new union set from a collection of elements.
     *
     * @param nodes
     *            the collection of elements
     * @return the root element
     */
    public V makeSet(Collection<V> nodes) {
        if (!nodes.isEmpty()) {
            Iterator<V> iterator = nodes.iterator();
            V root = makeSet(iterator.next());
            while (iterator.hasNext()) {
                root = union(root, iterator.next());
            }
            return root;
        } else {
            return null;
        }
    }

    /**
     * This method creates a single set containing the given node.
     *
     * @param node
     *            the root node of the set
     * @return the root element
     */
    public V makeSet(V node) {
        if (!nodeMap.containsKey(node)) {
            UnionFindNodeProperty<V> prop = new UnionFindNodeProperty<V>(0, node);
            nodeMap.put(node, prop);
            Set<V> set = new HashSet<V>();
            set.add(node);
            setMap.put(node, set);
        }
        return node;
    }

    /**
     * Find method with path compression.
     *
     * @param node
     *            the node to find
     * @return the root node of the set in which the given node can be found
     */
    public V find(V node) {
        UnionFindNodeProperty<V> prop = nodeMap.get(node);

        if (prop != null) {
            if (prop.parent.equals(node)) {
                return node;
            } else {
                prop.parent = find(prop.parent);
                return prop.parent;
            }
        }
        return null;
    }

    /**
     * Union by rank implementation of the two sets which contain x and y; x and/or y can be a single element from the
     * universe.
     *
     * @param x
     *            set or single element of the universe
     * @param y
     *            set or single element of the universe
     * @return the new root of the two sets
     */
    public V union(V x, V y) {
        V xRoot = find(x);
        V yRoot = find(y);

        if ((xRoot == null) || (yRoot == null)) {
            return union( (xRoot == null) ? makeSet(x) : xRoot, (yRoot == null) ? makeSet(y) : yRoot);
        }
        else if (!xRoot.equals(yRoot)) {
            UnionFindNodeProperty<V> xRootProp = nodeMap.get(xRoot);
            UnionFindNodeProperty<V> yRootProp = nodeMap.get(yRoot);

            if (xRootProp.rank < yRootProp.rank) {
                xRootProp.parent = yRoot;
                setMap.get(yRoot).addAll(setMap.get(xRoot));
                setMap.remove(xRoot);
                return yRoot;
            } else {// (xRootProp.rank >= yRootProp.rank)
                yRootProp.parent = xRoot;
                yRootProp.rank = (xRootProp.rank == yRootProp.rank) ? yRootProp.rank + 1 : yRootProp.rank;
                setMap.get(xRoot).addAll(setMap.get(yRoot));
                setMap.remove(yRoot);
                return xRoot;
            }
        } else {
            return xRoot;
        }
    }

    /**
     * Places the given elements in to the same partition.
     */
    public void unite(Set<V> elements) {
        if (elements.size() > 1) {
            V current = null;
            for (V element : elements) {
                if (current != null) {
                    if (getPartition(element) != null) {
                        union(current, element);
                    }
                } else {
                    if (getPartition(element) != null) {
                        current = element;
                    }
                }
            }
        }
    }

    /**
     * Delete the set whose root is the given node.
     *
     * @param root
     *            the root node
     */
    public void deleteSet(V root) {
        // if (setMap.containsKey(root))
        for (V n : setMap.get(root)) {
            nodeMap.remove(n);
        }
        setMap.remove(root);
    }

    /**
     * Returns if all given elements are in the same partition.
     */
    public boolean isSameUnion(Set<V> elements) {
        for (Set<V> partition : setMap.values()) {
            if (partition.containsAll(elements)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns the partition in which the given element can be found, or null otherwise.
     */
    public Set<V> getPartition(V element) {
        V root = find(element);
        return setMap.get(root);
    }

    /**
     * Returns all partitions.
     */
    public Collection<Set<V>> getPartitions() {
        return setMap.values();
    }

    public Set<V> getPartitionHeads() {
        return setMap.keySet();
    }
}
