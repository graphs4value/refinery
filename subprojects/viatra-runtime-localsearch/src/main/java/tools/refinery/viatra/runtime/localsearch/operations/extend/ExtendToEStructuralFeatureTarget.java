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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.exceptions.LocalSearchException;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;

/**
 * Iterates over all sources of {@link EStructuralFeature}
 */
public class ExtendToEStructuralFeatureTarget implements ISearchOperation {

    private class Executor extends SingleValueExtendOperationExecutor<Object>  {
        
        public Executor(int position) {
            super(position);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<?> getIterator(MatchingFrame frame, ISearchContext context) {
            try {
                final EObject value = (EObject) frame.getValue(sourcePosition);
                if(! feature.getEContainingClass().isSuperTypeOf(value.eClass()) ){
                    // TODO planner should ensure the proper supertype relation
                    return Collections.emptyIterator();
                }
                final Object featureValue = value.eGet(feature);
                if (feature.isMany()) {
                    if (featureValue != null) {
                        final Collection<Object> objectCollection = (Collection<Object>) featureValue;
                        return objectCollection.iterator();
                    } else {
                        return Collections.emptyIterator();
                    }
                } else {
                    if (featureValue != null) {
                        return Collections.singletonList(featureValue).iterator();
                    } else {
                        return Collections.emptyIterator();
                    }
                }
            } catch (ClassCastException e) {
                throw new LocalSearchException("Invalid feature source in parameter" + Integer.toString(sourcePosition), e);
            }
        }
        
        @Override
        public ISearchOperation getOperation() {
            return ExtendToEStructuralFeatureTarget.this;
        }
    }
    
    private final int sourcePosition;
    private final int targetPosition;
    private final EStructuralFeature feature;

    public ExtendToEStructuralFeatureTarget(int sourcePosition, int targetPosition, EStructuralFeature feature) {
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.feature = feature;
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    "+feature.getEContainingClass().getName()+"."+feature.getName()+"(+"+variableMapping.apply(sourcePosition)+", -"+ variableMapping.apply(targetPosition) +")";
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(targetPosition);
    }


    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(sourcePosition, targetPosition);
    }
    
}
