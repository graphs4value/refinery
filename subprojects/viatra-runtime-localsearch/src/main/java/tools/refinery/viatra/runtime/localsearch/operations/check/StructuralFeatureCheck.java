/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.operations.check;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.exceptions.LocalSearchException;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.CheckOperationExecutor;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;

/**
 * A simple operation that checks whether a {@link EStructuralFeature} connects two selected variables.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class StructuralFeatureCheck implements ISearchOperation {
    
    private class Executor extends CheckOperationExecutor {
        
        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            Objects.requireNonNull(frame.getValue(sourcePosition), () -> String.format("Invalid plan, variable %s unbound", sourcePosition));
            Objects.requireNonNull(frame.getValue(targetPosition), () -> String.format("Invalid plan, variable %s unbound", targetPosition));
            try {
                EObject source = (EObject) frame.getValue(sourcePosition);
                if(! feature.getEContainingClass().isSuperTypeOf(source.eClass()) ){
                    // TODO planner should ensure the proper supertype relation, see bug 500968
                    return false;
                }
                Object target = frame.getValue(targetPosition);
                if (feature.isMany()) {
                    return ((Collection<?>) source.eGet(feature)).contains(target);
                } else {
                    return target.equals(source.eGet(feature));
                }
            } catch (ClassCastException e) {
                throw new LocalSearchException(LocalSearchException.TYPE_ERROR, e);
            }
        }
        
        @Override
        public ISearchOperation getOperation() {
            return StructuralFeatureCheck.this;
        }
    }

    int sourcePosition;
    int targetPosition;
    EStructuralFeature feature;

    public StructuralFeatureCheck(int sourcePosition, int targetPosition, EStructuralFeature feature) {
        super();
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.feature = feature;
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
        return "check     "+feature.getContainerClass().getSimpleName()+"."+feature.getName()+"(+"+variableMapping.apply(sourcePosition)+", +"+variableMapping.apply(targetPosition)+")";
    }
    
    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(sourcePosition, targetPosition);
    }

}
