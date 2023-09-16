/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Marton Bur, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.plan.IPlanDescriptor;
import tools.refinery.interpreter.localsearch.plan.SearchPlan;
import tools.refinery.interpreter.localsearch.plan.SearchPlanExecutor;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.VolatileModifiableMaskedTuple;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * @author Zoltan Ujhelyi
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class LocalSearchMatcher implements ILocalSearchAdaptable {

    private final List<SearchPlanExecutor> plan;
    private final IPlanDescriptor planDescriptor;
    private final List<ILocalSearchAdapter> adapters;

    /**
     * @since 2.0
     */
    public List<SearchPlanExecutor> getPlan() {
        return plan;
    }

    @Override
    public List<ILocalSearchAdapter> getAdapters() {
        return new ArrayList<>(adapters);
    }

    private abstract class PlanExecutionIterator implements Iterator<Tuple> {

        protected final Iterator<SearchPlanExecutor> planIterator;

        protected SearchPlanExecutor currentPlan;
        protected MatchingFrame frame;
        protected final Set<ITuple> matchSet;
        protected VolatileModifiableMaskedTuple parametersOfFrameView;
        private boolean isNextMatchCalculated;

        public PlanExecutionIterator(final Iterator<SearchPlanExecutor> planIterator) {
            this.planIterator = planIterator;
            isNextMatchCalculated = false;
            matchSet = new HashSet<>();
        }

        protected boolean selectNextPlan() {
            if(currentPlan != null) {
                currentPlan.removeAdapters(adapters);
            }
            boolean validPlanSelected = false;

            SearchPlanExecutor nextPlan = null;

            while (!validPlanSelected && planIterator.hasNext()) {
                nextPlan = planIterator.next();
                nextPlan.addAdapters(adapters);
                nextPlan.resetPlan();

                validPlanSelected = initializeMatchingFrame(nextPlan);
            }

            if (validPlanSelected) {
                for (ILocalSearchAdapter adapter : adapters) {
                    adapter.planChanged(Optional.ofNullable(currentPlan).map(SearchPlanExecutor::getSearchPlan),
                            Optional.ofNullable(nextPlan).map(SearchPlanExecutor::getSearchPlan));
                }
                currentPlan = nextPlan;
                return true;
            } else {
                currentPlan = null;
                return false;
            }
        }

        protected abstract boolean initializeMatchingFrame(SearchPlanExecutor nextPlan);

        private boolean findNextNewMatchInCurrentPlan() {
            boolean foundMatch = currentPlan.execute(frame);
            while (foundMatch && matchSet.contains(parametersOfFrameView)) {
                for (ILocalSearchAdapter adapter : adapters) {
                    adapter.duplicateMatchFound(frame);
                }
                foundMatch = currentPlan.execute(frame);
            }
            return foundMatch;
        }

        @Override
        public boolean hasNext() {
            if (isNextMatchCalculated) {
                return true;
            }
            if (currentPlan == null) {
                return false;
            }
            boolean foundMatch = findNextNewMatchInCurrentPlan();

            while (!foundMatch && planIterator.hasNext()) {
                foundMatch = selectNextPlan() && findNextNewMatchInCurrentPlan();
            }
            if (!foundMatch) {
                for (ILocalSearchAdapter adapter : adapters) {
                    adapter.noMoreMatchesAvailable(LocalSearchMatcher.this);
                }
            }
            isNextMatchCalculated = foundMatch;
            return foundMatch;
        }

        @Override
        public Tuple next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more matches available.");
            }
            isNextMatchCalculated = false;
            final Tuple match = parametersOfFrameView.toImmutable();
            matchSet.add(match);
            return match;
        }
    }

    private class PlanExecutionIteratorWithArrayParameters extends PlanExecutionIterator {

        private final Object[] parameterValues;

        public PlanExecutionIteratorWithArrayParameters(Iterator<SearchPlanExecutor> planIterator, final Object[] parameterValues) {
            super(planIterator);
            this.parameterValues = parameterValues;
            selectNextPlan();
        }

        protected boolean initializeMatchingFrame(SearchPlanExecutor nextPlan) {
            frame = new MatchingFrame(nextPlan.getVariableMapping().size());
            parametersOfFrameView = new VolatileModifiableMaskedTuple(frame, nextPlan.getParameterMask());
            for (int i = 0; i < parameterValues.length; i++) {
                Object valueToSet = parameterValues[i];
                if (valueToSet != null) {
                    Object oldValue = parametersOfFrameView.get(i);
                    if (oldValue == null) {
                        parametersOfFrameView.set(i, valueToSet);
                    } else if (!Objects.equals(valueToSet, oldValue)) {
                        // Initial value setting resulted in contradictory values. This can happen because two parameter
                        // variables have been unified but the call provides different values for the parameters.
                        return false;
                    }
                    // If oldValue is not null but equal to newValue, the setting can be ignored
                }
            }

            return true;
        }
    }
    private class PlanExecutionIteratorWithTupleParameters extends PlanExecutionIterator {

        private final ITuple parameterValues;
        private final TupleMask parameterSeedMask;

        public PlanExecutionIteratorWithTupleParameters(Iterator<SearchPlanExecutor> planIterator, final TupleMask parameterSeedMask, final ITuple parameterValues) {
            super(planIterator);
            this.parameterSeedMask = parameterSeedMask;
            this.parameterValues = parameterValues;
            selectNextPlan();
        }

        protected boolean initializeMatchingFrame(SearchPlanExecutor nextPlan) {
            frame = new MatchingFrame(nextPlan.getVariableMapping().size());
            parametersOfFrameView = new VolatileModifiableMaskedTuple(frame, nextPlan.getParameterMask());
            for (int i = 0; i < parameterSeedMask.getSize(); i++) {
                int index = parameterSeedMask.indices[i];
                Object valueToSet = parameterValues.get(i);
                if (valueToSet != null) {
                    Object oldValue = parametersOfFrameView.get(index);
                    if (oldValue == null) {
                        parametersOfFrameView.set(index, valueToSet);
                    } else if (!Objects.equals(valueToSet, oldValue)) {
                        // Initial value setting resulted in contradictory values. This can happen because two parameter
                        // variables have been unified but the call provides different values for the parameters.
                        return false;
                    }
                    // If oldValue is not null but equal to newValue, the setting can be ignored
                }
            }

            return true;
        }
    }

    /**
     * @since 2.0
     */
    public LocalSearchMatcher(ISearchContext searchContext, IPlanDescriptor planDescriptor, List<SearchPlan> plan) {
        Preconditions.checkArgument(planDescriptor != null, "Cannot initialize matcher with null query.");
        this.planDescriptor = planDescriptor;
        this.plan = plan.stream().map(p -> new SearchPlanExecutor(p, searchContext)).collect(Collectors.toList());
        this.adapters = new LinkedList<>();
    }

    @Override
    public void addAdapter(ILocalSearchAdapter adapter) {
        this.adapters.add(adapter);
        adapter.adapterRegistered(this);
    }

    @Override
    public void removeAdapter(ILocalSearchAdapter adapter) {
        this.adapters.remove(adapter);
        adapter.adapterUnregistered(this);
    }

    @Override
    public void addAdapters(List<ILocalSearchAdapter> adapters) {
        this.adapters.addAll(adapters);
        for (ILocalSearchAdapter adapter : adapters) {
            adapter.adapterRegistered(this);
        }
    }

    @Override
    public void removeAdapters(List<ILocalSearchAdapter> adapters) {
        this.adapters.removeAll(adapters);
        for (ILocalSearchAdapter adapter : adapters) {
            adapter.adapterUnregistered(this);
        }
    }

    public int getParameterCount() {
        return planDescriptor.getQuery().getParameters().size();
    }

    private void matchingStarted() {
        for (ILocalSearchAdapter adapter : adapters) {
            adapter.patternMatchingStarted(this);
        }
    }

    /**
     * @since 2.0
     */
    public Stream<Tuple> streamMatches(final Object[] parameterValues) {
        matchingStarted();
        PlanExecutionIterator it = new PlanExecutionIteratorWithArrayParameters(plan.iterator(), parameterValues);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it,
                Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.DISTINCT), false);
    }

    /**
     * @since 2.0
     */
    public Stream<Tuple> streamMatches(TupleMask parameterSeedMask, final ITuple parameterValues) {
        matchingStarted();
        PlanExecutionIterator it = new PlanExecutionIteratorWithTupleParameters(
                plan.iterator(), parameterSeedMask, parameterValues);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it,
                Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.DISTINCT), false);
    }

    /**
     * Returns the query specification this matcher used as source for the implementation
     * @return never null
     */
    public PQuery getQuerySpecification() {
        return planDescriptor.getQuery();
    }


    /**
     * @since 1.5
     */
    public IPlanDescriptor getPlanDescriptor() {
        return planDescriptor;
    }
}
