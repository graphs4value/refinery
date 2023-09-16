/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem;

import java.util.Set;

import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * A constraint for which all satisfying tuples of variable values can be enumerated at any point during run-time.
 *
 * @author Gabor Bergmann
 *
 */
public abstract class EnumerablePConstraint extends BasePConstraint {
    protected Tuple variablesTuple;

    protected EnumerablePConstraint(PBody pBody, Tuple variablesTuple) {
        super(pBody, variablesTuple.<PVariable> getDistinctElements());
        this.variablesTuple = variablesTuple;
    }

    @Override
    public void doReplaceVariable(PVariable obsolete, PVariable replacement) {
        variablesTuple = variablesTuple.replaceAll(obsolete, replacement);
    }

    @Override
    protected String toStringRest() {
        String stringRestRest = toStringRestRest();
        String tupleString = "@" + variablesTuple.toString();
        return stringRestRest == null ? tupleString : ":" + stringRestRest + tupleString;
    }

    protected String toStringRestRest() {
        return null;
    }

    public Tuple getVariablesTuple() {
        return variablesTuple;
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return getAffectedVariables();
    }

    public PVariable getVariableInTuple(int index) {
        return (PVariable) this.variablesTuple.get(index);
    }

}
