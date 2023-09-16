/*******************************************************************************
 * Copyright (c) 2010-2012, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import tools.refinery.interpreter.rete.traceability.PatternTraceInfo;
import tools.refinery.interpreter.rete.traceability.TraceInfo;

/**
 * Base implementation for a Rete node.
 *
 * @author Bergmann Gabor
 *
 */
public abstract class BaseNode implements Node {

    protected ReteContainer reteContainer;
    protected long nodeId;
    protected Object tag;
    protected Set<TraceInfo> traceInfos;

    /**
     * @param reteContainer
     *            the container to create this node in
     */
    public BaseNode(ReteContainer reteContainer) {
        super();
        this.reteContainer = reteContainer;
        this.nodeId = reteContainer.registerNode(this);
        this.traceInfos = new HashSet<TraceInfo>();
    }

    @Override
    public String toString() {
        if (tag != null)
            return toStringCore() + "->" + getTraceInfoPatternsEnumerated() + "{" + tag.toString() + "}";
        else
            return toStringCore() + "->" + getTraceInfoPatternsEnumerated();
    }

    /**
     * clients should override this to append before the tag / trace indicators
     */
    protected String toStringCore() {
        return "[" + nodeId + "]" + getClass().getSimpleName();
    }

    @Override
    public ReteContainer getContainer() {
        return reteContainer;
    }

    @Override
    public long getNodeId() {
        return nodeId;
    }

    @Override
    public Object getTag() {
        return tag;
    }

    @Override
    public void setTag(Object tag) {
        this.tag = tag;
    }

    @Override
    public Set<TraceInfo> getTraceInfos() {
        return Collections.unmodifiableSet(traceInfos);
    }

    @Override
    public void assignTraceInfo(TraceInfo traceInfo) {
        traceInfos.add(traceInfo);
        traceInfo.assignNode(this);
    }

    @Override
    public void acceptPropagatedTraceInfo(TraceInfo traceInfo) {
        assignTraceInfo(traceInfo);
    }

    /**
     * Descendants should use this in e.g. logging
     */
    protected String getTraceInfoPatternsEnumerated() {
        TreeSet<String> patternNames = new TreeSet<String>();
        for (TraceInfo trInfo : traceInfos) {
            if (trInfo instanceof PatternTraceInfo) {
                final String pName = ((PatternTraceInfo) trInfo).getPatternName();
                patternNames.add(pName);
            }
        }
        return patternNames.toString();
    }

}
