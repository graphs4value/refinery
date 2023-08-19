/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.operations.extend;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClass;
import tools.refinery.viatra.runtime.emf.types.EClassTransitiveInstancesKey;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.IIteratingSearchOperation;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;

/**
 * Iterates all available {@link EClass} instances using an {@link IQueryRuntimeContext VIATRA Base indexer}. It is
 * assumed that the base indexer has been registered for the selected type.
 * 
 * @author Zoltan Ujhelyi
 * 
 */
public class IterateOverEClassInstances implements IIteratingSearchOperation {
    
    private class Executor extends SingleValueExtendOperationExecutor<Object> {
        
        public Executor(int position) {
            super(position);
        }

        @Override
        public Iterator<? extends Object> getIterator(MatchingFrame frame, ISearchContext context) {
            return context.getRuntimeContext().enumerateValues(type, indexerMask, Tuples.staticArityFlatTupleOf()).iterator();
        }
        
        @Override
        public ISearchOperation getOperation() {
            return IterateOverEClassInstances.this;
        }
    }

    private final EClass clazz;
    private final EClassTransitiveInstancesKey type;
    private static final TupleMask indexerMask = TupleMask.empty(1);
    private final int position;

    public IterateOverEClassInstances(int position, EClass clazz) {
        this.position = position;
        this.clazz = clazz;
        type = new EClassTransitiveInstancesKey(clazz);
    }

    public EClass getClazz() {
        return clazz;
    }

    
    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(position);
    }
    
    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    "+clazz.getName()+"(-"+ variableMapping.apply(position)+") indexed";
    }
    
    @Override
    public List<Integer> getVariablePositions() {
        return Collections.singletonList(position);
    }
    
    /**
     * @since 1.4
     */
    @Override
    public IInputKey getIteratedInputKey() {
        return type;
    }

}
