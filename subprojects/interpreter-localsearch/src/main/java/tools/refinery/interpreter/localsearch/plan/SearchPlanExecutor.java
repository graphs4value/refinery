/*******************************************************************************
 * Copyright (c) 2004-2008 Akos Horvath, Gergely Varro Zoltan Ujhelyi and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

 package tools.refinery.interpreter.localsearch.plan;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.localsearch.exceptions.LocalSearchException;
import tools.refinery.interpreter.localsearch.matcher.ILocalSearchAdaptable;
import tools.refinery.interpreter.localsearch.matcher.ILocalSearchAdapter;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * A search plan executor is used to execute {@link SearchPlan} instances.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class SearchPlanExecutor implements ILocalSearchAdaptable {

    private int currentOperation;
    private final List<ISearchOperation.ISearchOperationExecutor> operations;
    private final SearchPlan plan;
    private final ISearchContext context;
    private final List<ILocalSearchAdapter> adapters = new CopyOnWriteArrayList<>();

    /**
     * @since 2.0
     */
    public Map<Integer, PVariable> getVariableMapping() {
        return plan.getVariableMapping();
    }

    public int getCurrentOperation() {
        return currentOperation;
    }

    public SearchPlan getSearchPlan() {
        return plan;
    }

    /**
     * @since 1.7
     */
    public TupleMask getParameterMask() {
        return plan.getParameterMask();
    }

    @Override
    public void addAdapters(List<ILocalSearchAdapter> adapters) {
        for(ILocalSearchAdapter adapter : adapters){
            if (!this.adapters.contains(adapter)){
                this.adapters.add(adapter);
                adapter.adapterRegistered(this);
            }
        }
    }

    @Override
    public void removeAdapters(List<ILocalSearchAdapter> adapters) {
        for (ILocalSearchAdapter adapter : adapters) {
            if (this.adapters.remove(adapter)){
                adapter.adapterUnregistered(this);
            }
        }
    }

    /**
     * @since 2.0
     */
    public SearchPlanExecutor(SearchPlan plan, ISearchContext context) {
        Preconditions.checkArgument(context != null, "Context cannot be null");
        this.plan = plan;
        this.context = context;
        operations = plan.getOperations().stream().map(ISearchOperation::createExecutor).collect(Collectors.toList());
        this.currentOperation = -1;
    }


    private void init(MatchingFrame frame) {
        if (currentOperation == -1) {
            currentOperation++;
            ISearchOperation.ISearchOperationExecutor operation = operations.get(currentOperation);
            if (!adapters.isEmpty()){
                for (ILocalSearchAdapter adapter : adapters) {
                    adapter.executorInitializing(plan, frame);
                }
            }
            operation.onInitialize(frame, context);
        } else if (currentOperation == operations.size()) {
            currentOperation--;
        } else {
            throw new LocalSearchException(LocalSearchException.PLAN_EXECUTION_ERROR);
        }
    }


    /**
     * Calculates the cost of the search plan.
     */
    public double cost() {
        /* default generated stub */
        return 0.0;
    }

    /**
     * @throws InterpreterRuntimeException
     */
    public boolean execute(MatchingFrame frame) {
        int upperBound = operations.size() - 1;
        init(frame);
        operationSelected(frame, currentOperation, false);
        while (currentOperation >= 0 && currentOperation <= upperBound) {
            if (operations.get(currentOperation).execute(frame, context)) {
                operationExecuted(frame, currentOperation, true);
                currentOperation++;
                operationSelected(frame, currentOperation, false);
                if (currentOperation <= upperBound) {
                    ISearchOperation.ISearchOperationExecutor operation = operations.get(currentOperation);
                    operation.onInitialize(frame, context);
                }
            } else {
                operationExecuted(frame, currentOperation, false);
                ISearchOperation.ISearchOperationExecutor operation = operations.get(currentOperation);
                operation.onBacktrack(frame, context);
                currentOperation--;
                operationSelected(frame, currentOperation, true);
            }
        }
        boolean matchFound = currentOperation > upperBound;
        if (matchFound && !adapters.isEmpty()) {
            for (ILocalSearchAdapter adapter : adapters) {
                adapter.matchFound(plan, frame);
            }
        }
        return matchFound;
    }

    public void resetPlan() {
        currentOperation = -1;
    }

    public void printDebugInformation() {
        for (int i = 0; i < operations.size(); i++) {
            Logger.getRootLogger().debug("[" + i + "]\t" + operations.get(i).toString());
        }
    }

    private void operationExecuted(MatchingFrame frame, int operationIndex, boolean isSuccessful) {
        if (!adapters.isEmpty()){
            for (ILocalSearchAdapter adapter : adapters) {
                adapter.operationExecuted(plan, operations.get(operationIndex).getOperation(), frame, isSuccessful);
            }
        }
    }

    private void operationSelected(MatchingFrame frame, int operationIndex, boolean isBacktrack) {
        if (!adapters.isEmpty() && operationIndex >= 0 && operationIndex < operations.size()){
            for (ILocalSearchAdapter adapter : adapters) {
                adapter.operationSelected(plan, operations.get(operationIndex).getOperation(), frame, isBacktrack);
            }
        }
    }

    public ISearchContext getContext() {
        return context;
    }

    @Override
    public List<ILocalSearchAdapter> getAdapters() {
        return Collections.<ILocalSearchAdapter>unmodifiableList(this.adapters);
    }

    @Override
    public void addAdapter(ILocalSearchAdapter adapter) {
        addAdapters(Collections.singletonList(adapter));
    }

    @Override
    public void removeAdapter(ILocalSearchAdapter adapter) {
        removeAdapters(Collections.singletonList(adapter));
    }

    @Override
    public String toString() {
        if (operations == null) {
            return "Unspecified plan";
        } else {
            return operations.stream().map(Object::toString).collect(Collectors.joining("\n"));
        }
    }

}
