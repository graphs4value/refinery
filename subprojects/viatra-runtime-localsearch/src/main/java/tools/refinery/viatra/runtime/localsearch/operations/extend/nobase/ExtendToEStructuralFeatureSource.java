/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.operations.extend.nobase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.exceptions.LocalSearchException;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;
import tools.refinery.viatra.runtime.localsearch.operations.extend.SingleValueExtendOperationExecutor;

/**
 * Iterates over all sources of {@link EStructuralFeature} using an {@link NavigationHelper VIATRA Base indexer}.
 * It is assumed that the indexer is initialized for the selected {@link EStructuralFeature}.
 * 
 */
public class ExtendToEStructuralFeatureSource implements ISearchOperation {
    
    private class Executor extends SingleValueExtendOperationExecutor<Object> {
        
        private Executor() {
            super(sourcePosition);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public Iterator<?> getIterator(MatchingFrame frame, ISearchContext context) {
            if(!(feature instanceof EReference)){
                throw new LocalSearchException("Without base index, inverse navigation only possible along "
                        + "EReferences with defined EOpposite.");
            }
            EReference oppositeFeature = ((EReference)feature).getEOpposite();
            if(oppositeFeature == null){
                throw new LocalSearchException("Feature has no EOpposite, so cannot do inverse navigation " + feature.toString());            
            }
            try {
                final EObject value = (EObject) frame.getValue(targetPosition);
                if(! oppositeFeature.getEContainingClass().isSuperTypeOf(value.eClass()) ){
                    // TODO planner should ensure the proper supertype relation
                    return Collections.emptyIterator();
                }
                final Object featureValue = value.eGet(oppositeFeature);
                if (oppositeFeature.isMany()) {
                    if (featureValue != null) {
                        final Collection<Object> objectCollection = (Collection<Object>) featureValue;
                        return objectCollection.iterator();
                    } else {
                        return Collections.emptyIterator();
                    }
                } else {
                    if (featureValue != null) {
                        return Collections.singleton(featureValue).iterator();
                    } else {
                        return Collections.emptyIterator();
                    }
                }
            } catch (ClassCastException e) {
                throw new LocalSearchException("Invalid feature target in parameter" + Integer.toString(targetPosition), e);
            }
        }
        
        @Override
        public ISearchOperation getOperation() {
            return ExtendToEStructuralFeatureSource.this;
        }
    }

    private int targetPosition;
    private EStructuralFeature feature;
    private int sourcePosition;

    public ExtendToEStructuralFeatureSource(int sourcePosition, int targetPosition, EStructuralFeature feature) {
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.feature = feature;
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
        return "extend    "+feature.getContainerClass().getSimpleName()+"."+feature.getName()+"(-"+variableMapping.apply(sourcePosition)+", +"+variableMapping.apply(targetPosition)+") iterating";
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(sourcePosition, targetPosition);
    }
    
}
