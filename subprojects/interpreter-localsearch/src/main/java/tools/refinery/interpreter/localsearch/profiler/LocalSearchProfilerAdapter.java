/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.profiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ILocalSearchAdapter;
import tools.refinery.interpreter.localsearch.matcher.LocalSearchMatcher;
import tools.refinery.interpreter.localsearch.matcher.MatcherReference;
import tools.refinery.interpreter.localsearch.plan.SearchPlan;
import tools.refinery.interpreter.localsearch.plan.SearchPlanExecutor;

/**
 * This is a simple {@link ILocalSearchAdapter} which capable of counting
 * each search operation execution then printing it in human readably form
 * (along with the executed plans) using {@link #toString()}
 * @author Grill Balázs
 * @since 1.5
 *
 */
public class LocalSearchProfilerAdapter implements ILocalSearchAdapter {

    private final Map<MatcherReference, List<SearchPlan>> planReference = new HashMap<>();

    private final Map<ISearchOperation, Integer> successfulOperationCounts = new HashMap<>();
    private final Map<ISearchOperation, Integer> failedOperationCounts = new HashMap<>();

    @Override
    public void patternMatchingStarted(LocalSearchMatcher lsMatcher) {
        MatcherReference key = new MatcherReference(lsMatcher.getPlanDescriptor().getQuery(),
                lsMatcher.getPlanDescriptor().getAdornment());
        planReference.put(key, lsMatcher.getPlan().stream().map(SearchPlanExecutor::getSearchPlan).collect(Collectors.toList()));
    }

    @Override
    public void operationExecuted(SearchPlan plan, ISearchOperation operation, MatchingFrame frame, boolean isSuccessful) {
        Map<ISearchOperation, Integer> counts = isSuccessful ? successfulOperationCounts : failedOperationCounts;
        counts.merge(operation,
                /*no previous entry*/1,
                /*increase previous value*/(oldValue, v) -> oldValue + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<MatcherReference, List<SearchPlan>> entry: planReference.entrySet()){
            sb.append(entry.getKey());
            sb.append("\n");

            sb.append(entry.getValue());

            List<SearchPlan> bodies = entry.getValue();
            sb.append("{\n");
            for(int i=0;i<bodies.size();i++){
                sb.append("\tbody #");sb.append(i);sb.append("(\n");
                for(ISearchOperation operation : bodies.get(i).getOperations()){
                    final int successCount = successfulOperationCounts.computeIfAbsent(operation, op -> 0);
                    final int failCount = failedOperationCounts.computeIfAbsent(operation, op -> 0);
                    sb.append("\t\t");sb.append(successCount);
                    sb.append("\t");sb.append(failCount);
                    sb.append("\t");sb.append(successCount + failCount);
                    sb.append("\t");sb.append(operation);
                    sb.append("\n");
                }
                sb.append("\t)\n");
            }
            sb.append("}\n");
        }
        return sb.toString();
    }

}
