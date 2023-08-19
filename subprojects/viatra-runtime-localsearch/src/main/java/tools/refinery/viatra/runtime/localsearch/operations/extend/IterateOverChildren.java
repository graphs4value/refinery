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

import org.eclipse.emf.ecore.EObject;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;
import tools.refinery.viatra.runtime.matchers.util.Preconditions;

/**
 * Iterates all child elements of a selected EObjects. 
 * 
 * @author Zoltan Ujhelyi
 * 
 */
public class IterateOverChildren implements ISearchOperation {

    private class Executor extends SingleValueExtendOperationExecutor<EObject> {
        
        public Executor(int position) {
            super(position);
        }

        @Override
        public Iterator<EObject> getIterator(MatchingFrame frame, ISearchContext context) {
            Preconditions.checkState(frame.get(sourcePosition) instanceof EObject, "Only children of EObject elements are supported.");
            EObject source = (EObject) frame.get(sourcePosition);
            if(transitive) {
                return source.eAllContents();
            } else {
                return source.eContents().iterator();
            }
        }
        
        @Override
        public ISearchOperation getOperation() {
            return IterateOverChildren.this;
        }
    }

    private final int position;
    private int sourcePosition;
    private final boolean transitive;

    /**
     * 
     * @param position the position of the variable storing the child elements
     * @param sourcePosition the position of the variable storing the parent root; must be bound
     * @param transitive if true, child elements are iterated over transitively
     */
    public IterateOverChildren(int position, int sourcePosition, boolean transitive) {
        this.position = position;
        this.sourcePosition = sourcePosition;
        this.transitive = transitive;
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    containment +"+variableMapping.apply(sourcePosition)+" <>--> -"+variableMapping.apply(position)+(transitive ? " transitively" : " directly");
    }
    
    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(position);
    }


    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(position, sourcePosition);
    }

}