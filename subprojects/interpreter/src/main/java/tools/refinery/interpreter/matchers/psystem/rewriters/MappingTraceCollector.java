/*******************************************************************************
 * Copyright (c) 2010-2017, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.psystem.PTraceable;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.IMemoryView;
import tools.refinery.interpreter.matchers.util.IMultiLookup;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * Multimap-based implementation to contain and query traces
 *
 * @since 1.6
 *
 */
public class MappingTraceCollector implements IRewriterTraceCollector {

    /**
     * Traces from derivative to original
     */
    private final IMultiLookup<PTraceable, PTraceable> traces = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);

    /**
     * Traces from original to derivative
     */
    private final IMultiLookup<PTraceable, PTraceable> inverseTraces = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);

    /**
     * Reasons for removing {@link PTraceable}s
     */
    private final Map<PTraceable, IDerivativeModificationReason> removals = new HashMap<>();

    /**
     * Decides whether {@link PTraceable} is removed
     */
    private final Predicate<PTraceable> removed = removals::containsKey;

    /**
     * @since 2.0
     */
    @Override
    public Stream<PTraceable> getCanonicalTraceables(PTraceable derivative) {
        return findTraceEnds(derivative, traces).stream();
    }

    /**
     * @since 2.0
     */
    @Override
    public Stream<PTraceable> getRewrittenTraceables(PTraceable source) {
        return findTraceEnds(source, inverseTraces).stream();
    }

    /**
     * Returns the end of trace chains starting from the given {@link PTraceable} along the given trace edges.
     */
    private Set<PTraceable> findTraceEnds(PTraceable traceable, IMultiLookup<PTraceable, PTraceable> traceRecords) {
        if (traceable instanceof PQuery) { // PQueries are preserved
            return Collections.singleton(traceable);
        }
        Set<PTraceable> visited = new HashSet<>();
        Set<PTraceable> result = new HashSet<>();
        Queue<PTraceable> queue = new LinkedList<>();
        queue.add(traceable);
        while(!queue.isEmpty()){
            PTraceable aDerivative = queue.poll();
            // Track visited elements to avoid infinite loop via directed cycles in traces
            visited.add(aDerivative);
            IMemoryView<PTraceable> nextOrigins = traceRecords.lookup(aDerivative);
            if (nextOrigins == null){
                // End of trace chain
                result.add(aDerivative);
            } else {
                // Follow traces
                for(PTraceable nextOrigin : nextOrigins){
                    if (!visited.contains(nextOrigin)){
                        queue.add(nextOrigin);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void addTrace(PTraceable original, PTraceable derivative){
        traces.addPairOrNop(derivative, original);
        inverseTraces.addPairOrNop(original, derivative);
        // Even if this element was marked as removed earlier, now we replace it with another constraint!
        removals.remove(original);
    }

    @Override
    public void derivativeRemoved(PTraceable derivative, IDerivativeModificationReason reason){
        Preconditions.checkState(!removals.containsKey(derivative), "Traceable %s removed multiple times", derivative);
        // XXX the derivative must not be removed from the trace chain, as some rewriters, e.g. the normalizer keeps trace links to deleted elements
        if (!inverseTraces.lookupExists(derivative)) {
            // If there already exists a trace link, this removal means an update
            removals.put(derivative, reason);
        }
    }

    @Override
    public boolean isRemoved(PTraceable traceable) {
        return getRewrittenTraceables(traceable).allMatch(removed);
    }

    /**
     * @since 2.0
     */
    @Override
    public Stream<IDerivativeModificationReason> getRemovalReasons(PTraceable traceable) {
        return getRewrittenTraceables(traceable).filter(removed).map(removals::get);
    }

}
