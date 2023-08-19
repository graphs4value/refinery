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
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.exceptions.LocalSearchException;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.CheckOperationExecutor;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;

/**
 * A simple operation that checks whether a {@link EStructuralFeature} connects two selected variables.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ContainmentCheck implements ISearchOperation {

    private class Executor extends CheckOperationExecutor {
        
        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            try {
                EObject child = (EObject) frame.getValue(childPosition);
                EObject container = (EObject)frame.getValue(containerPosition);
                
                if (transitive) {
                    return EcoreUtil.isAncestor(container, child);
                } else {
                    return child.eContainer().equals(container);
                }
            } catch (ClassCastException e) {
                throw new LocalSearchException(LocalSearchException.TYPE_ERROR, e);
            }
        }
        
        @Override
        public ISearchOperation getOperation() {
            return ContainmentCheck.this;
        }
    }
    
    int childPosition;
    int containerPosition;
    private boolean transitive;

    public ContainmentCheck(int childPosition, int containerPosition, boolean transitive) {
        super();
        this.childPosition = childPosition;
        this.containerPosition = containerPosition;
        this.transitive = transitive;
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
        return "check     containment +"+variableMapping.apply(containerPosition)+" <>--> +"+childPosition+(transitive ? " transitively" : " directly");
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(childPosition, containerPosition);
    }

}
