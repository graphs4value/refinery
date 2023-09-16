/*******************************************************************************
 * Copyright (c) 2010-2015, Zoltan Ujhelyi, Marton Bur, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * Helper interface to ease the naming of the new variables during flattening
 *
 * @author Marton Bur
 *
 */
public interface IVariableRenamer {
    /**
     * Creates a variable name based on a given variable and a given query. It only creates a String, doesn't set
     * anything.
     *
     * @param pVariable
     * @param query
     * @return the new variable name as a String
     */
    String createVariableName(PVariable pVariable, PQuery query);

    public class SameName implements IVariableRenamer {
        @Override
        public String createVariableName(PVariable pVariable, PQuery query) {
            return pVariable.getName();
        }
    }

    public class HierarchicalName implements IVariableRenamer {

        private int callCount;

        public void setCallCount(int callCount) {
            this.callCount = callCount;
        }

        @Override
        public String createVariableName(PVariable pVariable, PQuery query) {
            // make sure to keep the "_" prefix before anonymous variables
            String newVarName = getShortName(query) + "<" + callCount + ">" + "_" + pVariable.getName();
            return pVariable.getName().startsWith("_") ? "_" + newVarName : newVarName ;
        }

        private String getShortName(PQuery query) {
            String fullyQualifiedName = query.getFullyQualifiedName();
            int beginIndex = fullyQualifiedName.lastIndexOf('.') + 1;
            return fullyQualifiedName.substring(beginIndex);
        }
    }
}
