/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication;

import java.util.Comparator;
import java.util.Map;

import tools.refinery.interpreter.rete.network.Node;

/**
 * @since 2.4
 */
public class NodeComparator implements Comparator<Node> {

    protected final Map<Node, Integer> nodeMap;

    public NodeComparator(final Map<Node, Integer> nodeMap) {
        this.nodeMap = nodeMap;
    }

    @Override
    public int compare(final Node left, final Node right) {
        return this.nodeMap.get(left) - this.nodeMap.get(right);
    }

}
