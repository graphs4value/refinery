/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.basicdeferred.TypeFilterConstraint;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.TypeConstraint;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.context.InputKeyImplication;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;

/**
 * A judgement that means that the given tuple of variables will represent a tuple of values that is a member of the extensional relation identified by the given input key.
 * @author Bergmann Gabor
 *
 */
public class TypeJudgement {

    private IInputKey inputKey;
    private Tuple variablesTuple;
    /**
     * @param inputKey
     * @param variablesTuple
     */
    public TypeJudgement(IInputKey inputKey, Tuple variablesTuple) {
        super();
        this.inputKey = inputKey;
        this.variablesTuple = variablesTuple;
    }
    public IInputKey getInputKey() {
        return inputKey;
    }
    public Tuple getVariablesTuple() {
        return variablesTuple;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((inputKey == null) ? 0 : inputKey.hashCode());
        result = prime * result
                + ((variablesTuple == null) ? 0 : variablesTuple.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof TypeJudgement))
            return false;
        TypeJudgement other = (TypeJudgement) obj;
        if (inputKey == null) {
            if (other.inputKey != null)
                return false;
        } else if (!inputKey.equals(other.inputKey))
            return false;
        if (variablesTuple == null) {
            if (other.variablesTuple != null)
                return false;
        } else if (!variablesTuple.equals(other.variablesTuple))
            return false;
        return true;
    }

    public Set<TypeJudgement> getDirectlyImpliedJudgements(IQueryMetaContext context) {
        Set<TypeJudgement> results = new HashSet<TypeJudgement>();
        results.add(this);

        Collection<InputKeyImplication> implications = context.getImplications(this.inputKey);
        for (InputKeyImplication inputKeyImplication : implications) {
            results.add(
                transcribeImplication(inputKeyImplication)
            );
        }

        return results;
    }

    /**
     * @since 1.6
     */
    public Set<TypeJudgement> getWeakenedAlternativeJudgements(IQueryMetaContext context) {
        Set<TypeJudgement> results = new HashSet<TypeJudgement>();

        Collection<InputKeyImplication> implications = context.getWeakenedAlternatives(this.inputKey);
        for (InputKeyImplication inputKeyImplication : implications) {
            results.add(
                transcribeImplication(inputKeyImplication)
            );
        }

        return results;
    }

    /**
     * @since 2.0
     */
    public Map<TypeJudgement, Set<TypeJudgement>> getConditionalImpliedJudgements(IQueryMetaContext context) {
        return context.getConditionalImplications(this.inputKey).entrySet().stream().collect(Collectors.toMap(
                entry -> transcribeImplication(entry.getKey()),
                entry -> entry.getValue().stream().map(this::transcribeImplication).collect(Collectors.toSet())));
    }



    private TypeJudgement transcribeImplication(InputKeyImplication inputKeyImplication) {
        return new TypeJudgement(
                inputKeyImplication.getImpliedKey(),
                transcribeVariablesToTuple(inputKeyImplication.getImpliedIndices())
        );
    }
    private Tuple transcribeVariablesToTuple(List<Integer> indices) {
        Object[] elements = new Object[indices.size()];
        for (int i = 0; i < indices.size(); ++i)
            elements[i] = variablesTuple.get(indices.get(i));
        return Tuples.flatTupleOf(elements);
    }

    @Override
    public String toString() {
        return "TypeJudgement:" + inputKey.getPrettyPrintableName() + "@" + variablesTuple.toString();
    }

    /**
     * Creates this judgement as a direct type constraint in the given PBody under construction.
     * <p> pre: the variables tuple must be formed of variables of that PBody.
     * @since 1.6
     */
    public PConstraint createConstraintFor(PBody pBody) {
        if (inputKey.isEnumerable()) {
            return new TypeConstraint(pBody, variablesTuple, inputKey);
        } else {
            return new TypeFilterConstraint(pBody, variablesTuple, inputKey);
        }
    }
}
