/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import tools.refinery.interpreter.localsearch.matcher.CallWithAdornment;
import tools.refinery.interpreter.localsearch.matcher.MatcherReference;
import tools.refinery.interpreter.matchers.psystem.IQueryReference;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.PatternCallBasedDeferred;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.BinaryReflexiveTransitiveClosure;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;

/**
 * This class stores a precompiled version of call-related metadata and masks for local search operations
 *
 * @author Zoltan Ujhelyi
 * @since 1.7
 */
public final class CallInformation {

    private final TupleMask fullFrameMask;
    private final TupleMask thinFrameMask;
    private final TupleMask parameterMask;
    private final int[] freeParameterIndices;

    private final Map<PParameter, Integer> mapping = new HashMap<>();
    private final Set<PParameter> adornment = new HashSet<>();
    private final PQuery referredQuery;
    private final MatcherReference matcherReference;
    private final IQueryReference call;
    private CallWithAdornment callWithAdornment;

    public static CallInformation create(PatternCallBasedDeferred constraint, Map<PVariable, Integer> variableMapping, Set<Integer> bindings) {
        return new CallInformation(constraint.getActualParametersTuple(), constraint, bindings, variableMapping);
    }

    public static CallInformation create(PositivePatternCall pCall, Map<PVariable, Integer> variableMapping, Set<Integer> bindings) {
        return new CallInformation(pCall.getVariablesTuple(), pCall, bindings, variableMapping);
    }

    public static CallInformation create(BinaryTransitiveClosure constraint, Map<PVariable, Integer> variableMapping, Set<Integer> bindings) {
        return new CallInformation(constraint.getVariablesTuple(), constraint, bindings, variableMapping);
    }

    /**
     * @since 2.0
     */
    public static CallInformation create(BinaryReflexiveTransitiveClosure constraint, Map<PVariable, Integer> variableMapping, Set<Integer> bindings) {
        return new CallInformation(constraint.getVariablesTuple(), constraint, bindings, variableMapping);
    }

    private CallInformation(Tuple actualParameters, IQueryReference call, final Set<Integer> bindings,
            Map<PVariable, Integer> variableMapping) {
        this.call = call;
        this.referredQuery = call.getReferredQuery();
        int keySize = actualParameters.getSize();
        List<Integer> parameterMaskIndices = new ArrayList<>();
        int[] fullParameterMaskIndices = new int[keySize];
        for (int i = 0; i < keySize; i++) {
            PParameter symbolicParameter = referredQuery.getParameters().get(i);
            PVariable parameter = (PVariable) actualParameters.get(i);
            Integer originalFrameIndex = variableMapping.get(parameter);
            mapping.put(symbolicParameter, originalFrameIndex);
            fullParameterMaskIndices[i] = originalFrameIndex;
            if (bindings.contains(originalFrameIndex)) {
                parameterMaskIndices.add(originalFrameIndex);
                adornment.add(symbolicParameter);
            }
        }

        thinFrameMask = TupleMask.fromSelectedIndices(variableMapping.size(), parameterMaskIndices);
        fullFrameMask = TupleMask.fromSelectedIndices(variableMapping.size(), fullParameterMaskIndices);

        // This second iteration is necessary as we don't know beforehand the number of bound parameters
        int[] boundParameterIndices = new int[adornment.size()];
        int boundIndex = 0;
        freeParameterIndices = new int[keySize - adornment.size()];
        int freeIndex = 0;
        for (int i = 0; i < keySize; i++) {
            if (bindings.contains(variableMapping.get(actualParameters.get(i)))) {
                boundParameterIndices[boundIndex] = i;
                boundIndex++;
            } else {
                freeParameterIndices[freeIndex] = i;
                freeIndex++;
            }
        }
        parameterMask = TupleMask.fromSelectedIndices(keySize, boundParameterIndices);
        callWithAdornment = new CallWithAdornment(call, adornment);
        matcherReference = callWithAdornment.getMatcherReference();
    }

    /**
     * Returns a mask describing how the bound variables of a Matching Frame are mapped to parameter indexes
     */
    public TupleMask getThinFrameMask() {
        return thinFrameMask;
    }

    /**
     * Returns a mask describing how all variables of a Matching Frame are mapped to parameter indexes
     */
    public TupleMask getFullFrameMask() {
        return fullFrameMask;
    }

    /**
     * Returns a mask describing the adornment the called pattern uses
     */
    public TupleMask getParameterMask() {
        return parameterMask;
    }

    public MatcherReference getReference() {
        return matcherReference;
    }

    /**
     * @since 2.1
     */
    public IQueryReference getCall() {
        return call;
    }

    /**
     * @since 2.1
     */
    public CallWithAdornment getCallWithAdornment() {
        return callWithAdornment;
    }

    /**
     * Returns the parameter indices that are unbound before the call
     */
    public int[] getFreeParameterIndices() {
        return freeParameterIndices;
    }

    public List<Integer> getVariablePositions() {
        List<Integer> variables = new ArrayList<>(mapping.size());
        for(PParameter p : referredQuery.getParameters()){
            variables.add(mapping.get(p));
        }
        return variables;
    }



    @Override
    public String toString() {
        return toString(Object::toString);
    }

    /**
     * @since 2.0
     */
    public String toString(Function<Integer, String> variableMapping) {
        return referredQuery.getFullyQualifiedName() + "("
                + referredQuery.getParameters().stream().map(
                        input -> (adornment.contains(input) ? "+" : "-") + variableMapping.apply(mapping.get(input)))
                        .collect(Collectors.joining(","))
                + ")";
    }


}
