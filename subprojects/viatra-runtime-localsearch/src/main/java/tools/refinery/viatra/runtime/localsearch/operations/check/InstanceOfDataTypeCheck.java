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
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.emf.ecore.EDataType;
import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.CheckOperationExecutor;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;

/**
 * @author Zoltan Ujhelyi
 * @noextend This class is not intended to be subclassed by clients.
 */
public class InstanceOfDataTypeCheck implements ISearchOperation {

    private class Executor extends CheckOperationExecutor {
        
        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            Objects.requireNonNull(frame.getValue(position), () -> String.format("Invalid plan, variable %s unbound", position));
            return dataType.isInstance(frame.getValue(position));
        }
        
        @Override
        public ISearchOperation getOperation() {
            return InstanceOfDataTypeCheck.this;
        }
    }
    
    private int position;
    private EDataType dataType;

    public InstanceOfDataTypeCheck(int position, EDataType dataType) {
        this.position = position;
        this.dataType = dataType;

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
        return "check     "+dataType.getName()+"(+"+variableMapping.apply(position)+")";
    }
    
    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(position);
    }
    
}
