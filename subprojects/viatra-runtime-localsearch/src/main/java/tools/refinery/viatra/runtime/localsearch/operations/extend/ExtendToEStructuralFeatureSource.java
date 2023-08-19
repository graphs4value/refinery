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
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.emf.types.EStructuralFeatureInstancesKey;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.IIteratingSearchOperation;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.VolatileMaskedTuple;

/**
 * Iterates over all sources of {@link EStructuralFeature} using an {@link IQueryRuntimeContext VIATRA Base indexer}.
 * It is assumed that the indexer is initialized for the selected {@link EStructuralFeature}.
 * 
 */
public class ExtendToEStructuralFeatureSource implements IIteratingSearchOperation {

    private class Executor extends SingleValueExtendOperationExecutor<EObject> {
        
        private VolatileMaskedTuple maskedTuple;
        
        public Executor(int position) {
            super(position);
            this.maskedTuple = new VolatileMaskedTuple(mask);
        }

        
        @Override
        public Iterator<EObject> getIterator(MatchingFrame frame, ISearchContext context) {
            maskedTuple.updateTuple(frame);
            Iterable<? extends Object> values = context.getRuntimeContext().enumerateValues(type, indexerMask, maskedTuple);
            return StreamSupport.stream(values.spliterator(), false)
                    .filter(EObject.class::isInstance)
                    .map(EObject.class::cast)
                    .iterator();
        }
        
        @Override
        public ISearchOperation getOperation() {
            return ExtendToEStructuralFeatureSource.this;
        }
    }
    
    private final int sourcePosition;
    private final int targetPosition;
    private final EStructuralFeature feature;
    private final IInputKey type;
    private static final TupleMask indexerMask = TupleMask.fromSelectedIndices(2, new int[] {1});
    private final TupleMask mask;
    
    /**
     * @since 1.7
     */
    public ExtendToEStructuralFeatureSource(int sourcePosition, int targetPosition, EStructuralFeature feature, TupleMask mask) {
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.feature = feature;
        this.mask = mask;
        this.type = new EStructuralFeatureInstancesKey(feature);
    }

    public EStructuralFeature getFeature() {
        return feature;
    }
    
    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(sourcePosition);
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    "+feature.getContainerClass().getSimpleName()+"."+feature.getName()+"(-"+variableMapping.apply(sourcePosition)+", +"+variableMapping.apply(targetPosition)+") indexed";
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
