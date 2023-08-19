/*******************************************************************************
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.operations.extend.nobase;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EClass;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.emf.types.EClassTransitiveInstancesKey;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.IIteratingSearchOperation;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;

/**
 * Iterates all available {@link EClass} instances without using an {@link NavigationHelper VIATRA Base indexer}.
 * 
 * @author Zoltan Ujhelyi
 */
public class IterateOverEClassInstances implements IIteratingSearchOperation {
    
    private class Executor extends AbstractIteratingExtendOperationExecutor<Notifier> {
        
        public Executor(int position, EMFScope scope) {
            super(position, scope);
        }

        @Override
        public Iterator<? extends Notifier> getIterator(MatchingFrame frame, ISearchContext context) {
            return getModelContents().filter(clazz::isInstance).iterator();
        }
        
        @Override
        public ISearchOperation getOperation() {
            return IterateOverEClassInstances.this;
        }
    }

    private final int position;
    private final EClass clazz;
    private final EMFScope scope;

    public IterateOverEClassInstances(int position, EClass clazz, EMFScope scope) {
        this.position = position;
        this.clazz = clazz;
        this.scope = scope;
    }

    public EClass getClazz() {
        return clazz;
    }
    
    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(position, scope);
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }
    
    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    "+clazz.getName()+"(-"+ variableMapping.apply(position)+") iterating";
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
        return new EClassTransitiveInstancesKey(clazz);
    }
    
}
