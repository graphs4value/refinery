/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.psystem.EnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.*;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.*;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class can create a new PBody for a PQuery. The result body contains a copy of given variables and constraints.
 *
 * @author Marton Bur
 *
 */
public class PBodyCopier extends AbstractRewriterTraceSource {

    /**
     * The created body
     */
    protected PBody body;
    /**
     * Mapping between the original and the copied variables
     */
    protected Map<PVariable, PVariable> variableMapping = new HashMap<>();

    public Map<PVariable, PVariable> getVariableMapping() {
        return variableMapping;
    }

    /**
     * @since 1.6
     */
    public PBodyCopier(PBody body, IRewriterTraceCollector traceCollector) {
        this.body = new PBody(body.getPattern());
        setTraceCollector(traceCollector);

        // do the actual copying
        mergeBody(body);
    }

    /**
     * @since 1.6
     */
    public PBodyCopier(PQuery query) {
        this.body = new PBody(query);
    }

    public void mergeBody(PBody sourceBody) {
        mergeBody(sourceBody, new IVariableRenamer.SameName(), new IConstraintFilter.AllowAllFilter());
    }

    /**
     * Merge all variables and constraints from a source body to a target body. If multiple bodies are merged into a
     * single one, use the renamer and filter options to avoid collisions.
     */
    public void mergeBody(PBody sourceBody, IVariableRenamer namingTool, IConstraintFilter filter) {

        // Copy variables
        Set<PVariable> allVariables = sourceBody.getAllVariables();
        for (PVariable pVariable : allVariables) {
            if (pVariable.isUnique()) {
                copyVariable(pVariable, namingTool.createVariableName(pVariable, sourceBody.getPattern()));
            }
        }

        // Copy exported parameters
        this.body.setSymbolicParameters(sourceBody.getSymbolicParameters().stream()
                .map(this::copyExportedParameterConstraint).collect(Collectors.toList()));

        // Copy constraints which are not filtered
        Set<PConstraint> constraints = sourceBody.getConstraints();
        for (PConstraint pConstraint : constraints) {
            if (!(pConstraint instanceof ExportedParameter) && !filter.filter(pConstraint)) {
                copyConstraint(pConstraint);
            }
        }

        // Add trace between original and copied body
        addTrace(sourceBody, body);
    }

    protected void copyVariable(PVariable variable, String newName) {
        PVariable newPVariable = body.getOrCreateVariableByName(newName);
        variableMapping.put(variable, newPVariable);
    }

    /**
     * Returns the body with the copied variables and constraints. The returned body is still uninitialized.
     */
    public PBody getCopiedBody() {
        return body;
    }

    protected void copyConstraint(PConstraint constraint) {
        if (constraint instanceof ExportedParameter) {
            copyExportedParameterConstraint((ExportedParameter) constraint);
        } else if (constraint instanceof Equality) {
            copyEqualityConstraint((Equality) constraint);
        } else if (constraint instanceof Inequality) {
            copyInequalityConstraint((Inequality) constraint);
        } else if (constraint instanceof TypeConstraint) {
            copyTypeConstraint((TypeConstraint) constraint);
        } else if (constraint instanceof TypeFilterConstraint) {
            copyTypeFilterConstraint((TypeFilterConstraint) constraint);
        } else if (constraint instanceof ConstantValue) {
            copyConstantValueConstraint((ConstantValue) constraint);
        } else if (constraint instanceof PositivePatternCall) {
            copyPositivePatternCallConstraint((PositivePatternCall) constraint);
        } else if (constraint instanceof NegativePatternCall) {
            copyNegativePatternCallConstraint((NegativePatternCall) constraint);
        } else if (constraint instanceof BinaryTransitiveClosure) {
            copyBinaryTransitiveClosureConstraint((BinaryTransitiveClosure) constraint);
		} else if (constraint instanceof RepresentativeElectionConstraint) {
			copyRepresentativeElectionConstraint((RepresentativeElectionConstraint) constraint);
        } else if (constraint instanceof RelationEvaluation) {
            copyRelationEvaluationConstraint((RelationEvaluation) constraint);
        } else if (constraint instanceof BinaryReflexiveTransitiveClosure) {
            copyBinaryReflexiveTransitiveClosureConstraint((BinaryReflexiveTransitiveClosure) constraint);
        } else if (constraint instanceof PatternMatchCounter) {
            copyPatternMatchCounterConstraint((PatternMatchCounter) constraint);
        } else if (constraint instanceof AggregatorConstraint) {
            copyAggregatorConstraint((AggregatorConstraint) constraint);
        } else if (constraint instanceof ExpressionEvaluation) {
            copyExpressionEvaluationConstraint((ExpressionEvaluation) constraint);
        } else {
            throw new QueryProcessingException("Unknown PConstraint {0} encountered while copying PBody",
                    new String[] { constraint.getClass().getName() }, "Unknown PConstraint", body.getPattern());
        }
    }

    protected ExportedParameter copyExportedParameterConstraint(ExportedParameter exportedParameter) {
        PVariable mappedPVariable = variableMapping.get(exportedParameter.getParameterVariable());
        PParameter parameter = exportedParameter.getPatternParameter();
        ExportedParameter newExportedParameter;
        newExportedParameter = new ExportedParameter(body, mappedPVariable, parameter);
        body.getSymbolicParameters().add(newExportedParameter);
        addTrace(exportedParameter, newExportedParameter);
        return newExportedParameter;
    }

    protected void copyEqualityConstraint(Equality equality) {
        PVariable who = equality.getWho();
        PVariable withWhom = equality.getWithWhom();
        addTrace(equality, new Equality(body, variableMapping.get(who), variableMapping.get(withWhom)));
    }

    protected void copyInequalityConstraint(Inequality inequality) {
        PVariable who = inequality.getWho();
        PVariable withWhom = inequality.getWithWhom();
        addTrace(inequality, new Inequality(body, variableMapping.get(who), variableMapping.get(withWhom)));
    }

    protected void copyTypeConstraint(TypeConstraint typeConstraint) {
        PVariable[] mappedVariables = extractMappedVariables(typeConstraint);
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(typeConstraint, new TypeConstraint(body, variablesTuple, typeConstraint.getSupplierKey()));
    }

    protected void copyTypeFilterConstraint(TypeFilterConstraint typeConstraint) {
        PVariable[] mappedVariables = extractMappedVariables(typeConstraint);
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(typeConstraint, new TypeFilterConstraint(body, variablesTuple, typeConstraint.getInputKey()));
    }

    protected void copyConstantValueConstraint(ConstantValue constantValue) {
        PVariable pVariable = (PVariable) constantValue.getVariablesTuple().getElements()[0];
        addTrace(constantValue,
                new ConstantValue(body, variableMapping.get(pVariable), constantValue.getSupplierKey()));
    }

    protected void copyPositivePatternCallConstraint(PositivePatternCall positivePatternCall) {
        PVariable[] mappedVariables = extractMappedVariables(positivePatternCall);
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(positivePatternCall,
                new PositivePatternCall(body, variablesTuple, positivePatternCall.getReferredQuery()));
    }

    protected void copyNegativePatternCallConstraint(NegativePatternCall negativePatternCall) {
        PVariable[] mappedVariables = extractMappedVariables(negativePatternCall);
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(negativePatternCall,
                new NegativePatternCall(body, variablesTuple, negativePatternCall.getReferredQuery()));
    }

    protected void copyBinaryTransitiveClosureConstraint(BinaryTransitiveClosure binaryTransitiveClosure) {
        PVariable[] mappedVariables = extractMappedVariables(binaryTransitiveClosure);
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(binaryTransitiveClosure,
                new BinaryTransitiveClosure(body, variablesTuple, binaryTransitiveClosure.getReferredQuery()));
    }

	protected void copyRepresentativeElectionConstraint(RepresentativeElectionConstraint constraint) {
		var mappedVariables = extractMappedVariables(constraint);
		var variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
		addTrace(constraint, new RepresentativeElectionConstraint(body, variablesTuple, constraint.getReferredQuery(),
				constraint.getConnectivity()));
	}

    /**
     * @since 2.8
     */
    protected void copyRelationEvaluationConstraint(RelationEvaluation relationEvaluation) {
        PVariable[] mappedVariables = extractMappedVariables(relationEvaluation);
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(relationEvaluation, new RelationEvaluation(body, variablesTuple, relationEvaluation.getReferredQueries(),
                relationEvaluation.getEvaluator()));
    }

    /**
     * @since 2.0
     */
    protected void copyBinaryReflexiveTransitiveClosureConstraint(
            BinaryReflexiveTransitiveClosure binaryReflexiveTransitiveClosure) {
        PVariable[] mappedVariables = extractMappedVariables(binaryReflexiveTransitiveClosure);
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(binaryReflexiveTransitiveClosure,
                new BinaryReflexiveTransitiveClosure(body, variablesTuple,
                        binaryReflexiveTransitiveClosure.getReferredQuery(),
                        binaryReflexiveTransitiveClosure.getUniverseType()));
    }

    protected void copyPatternMatchCounterConstraint(PatternMatchCounter patternMatchCounter) {
        PVariable[] mappedVariables = extractMappedVariables(patternMatchCounter);
        PVariable mappedResultVariable = variableMapping.get(patternMatchCounter.getResultVariable());
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(patternMatchCounter, new PatternMatchCounter(body, variablesTuple,
                patternMatchCounter.getReferredQuery(), mappedResultVariable));
    }

    /**
     * @since 1.4
     */
    protected void copyAggregatorConstraint(AggregatorConstraint constraint) {
        PVariable[] mappedVariables = extractMappedVariables(constraint);
        PVariable mappedResultVariable = variableMapping.get(constraint.getResultVariable());
        Tuple variablesTuple = Tuples.flatTupleOf((Object[]) mappedVariables);
        addTrace(constraint, new AggregatorConstraint(constraint.getAggregator(), body, variablesTuple,
                constraint.getReferredQuery(), mappedResultVariable, constraint.getAggregatedColumn()));
    }

    protected void copyExpressionEvaluationConstraint(ExpressionEvaluation expressionEvaluation) {
        PVariable mappedOutputVariable = variableMapping.get(expressionEvaluation.getOutputVariable());
        addTrace(expressionEvaluation, new ExpressionEvaluation(body,
                new VariableMappingExpressionEvaluatorWrapper(expressionEvaluation.getEvaluator(), variableMapping),
                mappedOutputVariable, expressionEvaluation.isUnwinding()));
    }

    /**
     * For positive pattern calls
     *
     * @param positivePatternCall
     * @return the mapped variables to the pattern's parameters
     */
    protected PVariable[] extractMappedVariables(EnumerablePConstraint enumerablePConstraint) {
        Object[] pVariables = enumerablePConstraint.getVariablesTuple().getElements();
        return mapVariableList(pVariables);
    }

    /**
     * For negative and count pattern calls.
     *
     * @param patternMatchCounter
     * @return the mapped variables to the pattern's parameters
     */
    private PVariable[] extractMappedVariables(PatternCallBasedDeferred patternCallBasedDeferred) {
        Object[] pVariables = patternCallBasedDeferred.getActualParametersTuple().getElements();
        return mapVariableList(pVariables);
    }

    /**
     * For type filters.
     */
    private PVariable[] extractMappedVariables(TypeFilterConstraint typeFilterConstraint) {
        Object[] pVariables = typeFilterConstraint.getVariablesTuple().getElements();
        return mapVariableList(pVariables);
    }

    private PVariable[] mapVariableList(Object[] pVariables) {
        List<PVariable> list = new ArrayList<PVariable>();
        for (int i = 0; i < pVariables.length; i++) {
            PVariable mappedVariable = variableMapping.get(pVariables[i]);
            list.add(mappedVariable);
        }
        return list.toArray(new PVariable[0]);
    }

}
