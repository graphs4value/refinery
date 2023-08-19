/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.operations.extend;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.emf.types.EStructuralFeatureInstancesKey;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.IIteratingSearchOperation;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;

/**
 * Iterates all available {@link EStructuralFeature} elements using an {@link IQueryRuntimeContext VIATRA Base
 * indexer}. It is assumed that the base indexer has been registered for the selected reference type.
 * 
 */
public class IterateOverEStructuralFeatureInstances implements IIteratingSearchOperation{

    private class Executor implements ISearchOperationExecutor {
        private Iterator<Tuple> it;

        @Override
        public void onBacktrack(MatchingFrame frame, ISearchContext context) {
            frame.setValue(sourcePosition, null);
            frame.setValue(targetPosition, null);
            it = null;
        }

        @Override
        public void onInitialize(MatchingFrame frame, ISearchContext context) {
            Iterable<Tuple> tuples = context.getRuntimeContext().enumerateTuples(type, indexerMask, Tuples.staticArityFlatTupleOf());

            it = tuples.iterator();
        }

        @Override
        public boolean execute(MatchingFrame frame, ISearchContext context) {
            if (it.hasNext()) {
                final Tuple next = it.next();
                frame.setValue(sourcePosition, next.get(0));
                frame.setValue(targetPosition, next.get(1));
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public ISearchOperation getOperation() {
            return IterateOverEStructuralFeatureInstances.this;
        }
    }
    
    private final EStructuralFeature feature;
    private final int sourcePosition;
    private final int targetPosition;
    private final EStructuralFeatureInstancesKey type;
    private static final TupleMask indexerMask = TupleMask.empty(2);
    
    public IterateOverEStructuralFeatureInstances(int sourcePosition, int targetPosition, EStructuralFeature feature) {
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.feature = feature;
        type = new EStructuralFeatureInstancesKey(feature);
    }
    
    public EStructuralFeature getFeature() {
        return feature;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }
    
    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    "+feature.getContainerClass().getSimpleName()+"."+feature.getName()+"(-"+variableMapping.apply(sourcePosition)+", -"+variableMapping.apply(targetPosition)+") indexed";
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(sourcePosition, targetPosition);
    }

    /**
     * @since 1.4
     */
    @Override
    public IInputKey getIteratedInputKey() {
        return type;
    }
    
}
