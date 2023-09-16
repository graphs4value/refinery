/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.util.CallInformation;
import tools.refinery.interpreter.localsearch.matcher.CallWithAdornment;
import tools.refinery.interpreter.localsearch.operations.check.AggregatorCheck;
import tools.refinery.interpreter.localsearch.operations.check.BinaryTransitiveClosureCheck;
import tools.refinery.interpreter.localsearch.operations.check.CheckConstant;
import tools.refinery.interpreter.localsearch.operations.check.CheckPositivePatternCall;
import tools.refinery.interpreter.localsearch.operations.check.CountCheck;
import tools.refinery.interpreter.localsearch.operations.check.ExpressionCheck;
import tools.refinery.interpreter.localsearch.operations.check.ExpressionEvalCheck;
import tools.refinery.interpreter.localsearch.operations.check.InequalityCheck;
import tools.refinery.interpreter.localsearch.operations.check.NACOperation;
import tools.refinery.interpreter.localsearch.operations.extend.AggregatorExtend;
import tools.refinery.interpreter.localsearch.operations.extend.CountOperation;
import tools.refinery.interpreter.localsearch.operations.extend.ExpressionEval;
import tools.refinery.interpreter.localsearch.operations.extend.ExtendBinaryTransitiveClosure;
import tools.refinery.interpreter.localsearch.operations.extend.ExtendConstant;
import tools.refinery.interpreter.localsearch.operations.extend.ExtendPositivePatternCall;
import tools.refinery.interpreter.localsearch.planner.util.CompilerHelper;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.operations.PApply;
import tools.refinery.interpreter.matchers.planning.operations.POperation;
import tools.refinery.interpreter.matchers.planning.operations.PProject;
import tools.refinery.interpreter.matchers.planning.operations.PStart;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.AggregatorConstraint;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExpressionEvaluation;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.Inequality;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.NegativePatternCall;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.PatternMatchCounter;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.TypeFilterConstraint;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.BinaryReflexiveTransitiveClosure;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.ConstantValue;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.TypeConstraint;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public abstract class AbstractOperationCompiler implements IOperationCompiler {

    protected static final String UNSUPPORTED_TYPE_MESSAGE = "Unsupported type: ";

    protected abstract void createExtend(TypeConstraint typeConstraint, Map<PVariable, Integer> variableMapping);

    /**
     * @throws InterpreterRuntimeException
     */
    protected abstract void createCheck(TypeConstraint typeConstraint, Map<PVariable, Integer> variableMapping);

    /**
     * @throws InterpreterRuntimeException
     */
    protected abstract void createCheck(TypeFilterConstraint typeConstraint, Map<PVariable, Integer> variableMapping);

    /**
     * @since 2.0
     * @throws InterpreterRuntimeException
     */
    protected abstract void createUnaryTypeCheck(IInputKey type, int position);

    protected List<ISearchOperation> operations;
    protected Set<CallWithAdornment> dependencies = new HashSet<>();
    protected Map<PConstraint, Set<Integer>> variableBindings;
    private Map<PVariable, Integer> variableMappings;
    protected final IQueryRuntimeContext runtimeContext;

    public AbstractOperationCompiler(IQueryRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    /**
     * Compiles a plan of <code>POperation</code>s to a list of type <code>List&ltISearchOperation></code>
     *
     * @param plan
     * @param boundParameters
     * @return an ordered list of POperations that make up the compiled search plan
     * @throws InterpreterRuntimeException
     */
    @Override
    public List<ISearchOperation> compile(SubPlan plan, Set<PParameter> boundParameters) {

        variableMappings = CompilerHelper.createVariableMapping(plan);
        variableBindings = CompilerHelper.cacheVariableBindings(plan, variableMappings, boundParameters);

        operations = new ArrayList<>();

        List<POperation> operationList = CompilerHelper.createOperationsList(plan);
        for (POperation pOperation : operationList) {
            compile(pOperation, variableMappings);
        }

        return operations;
    }

    private void compile(POperation pOperation, Map<PVariable, Integer> variableMapping) {

        if (pOperation instanceof PApply) {
            PApply pApply = (PApply) pOperation;
            PConstraint pConstraint = pApply.getPConstraint();

            if (isCheck(pConstraint, variableMapping)) {
                // check
                createCheckDispatcher(pConstraint, variableMapping);
            } else {
                // extend
                createExtendDispatcher(pConstraint, variableMapping);
            }

        } else if (pOperation instanceof PStart) {
            // nop
        } else if (pOperation instanceof PProject) {
            // nop
        } else {
            throw new QueryProcessingException("PStart, PApply or PProject was expected, received: " + pOperation.getClass(), null,"Unexpected POperation type", null);
        }

    }

    private void createCheckDispatcher(PConstraint pConstraint, Map<PVariable, Integer> variableMapping) {


        // DeferredPConstraint subclasses

        // Equalities are normalized

        if (pConstraint instanceof Inequality) {
            createCheck((Inequality) pConstraint, variableMapping);
        } else if (pConstraint instanceof PositivePatternCall){
            createCheck((PositivePatternCall) pConstraint, variableMapping);
        } else if (pConstraint instanceof NegativePatternCall) {
            createCheck((NegativePatternCall) pConstraint,variableMapping);
        } else if (pConstraint instanceof AggregatorConstraint) {
            createCheck((AggregatorConstraint) pConstraint, variableMapping);
        } else if (pConstraint instanceof PatternMatchCounter) {
            createCheck((PatternMatchCounter) pConstraint, variableMapping);
        } else if (pConstraint instanceof ExpressionEvaluation) {
            createCheck((ExpressionEvaluation) pConstraint, variableMapping);
        } else if (pConstraint instanceof TypeFilterConstraint) {
            createCheck((TypeFilterConstraint) pConstraint,variableMapping);
        } else if (pConstraint instanceof ExportedParameter) {
            // Nothing to do here
        } else

        // EnumerablePConstraint subclasses

        if (pConstraint instanceof BinaryTransitiveClosure) {
            createCheck((BinaryTransitiveClosure) pConstraint, variableMapping);
        } else if (pConstraint instanceof BinaryReflexiveTransitiveClosure) {
            createCheck((BinaryReflexiveTransitiveClosure)pConstraint, variableMapping);
        } else if (pConstraint instanceof ConstantValue) {
            createCheck((ConstantValue) pConstraint, variableMapping);
        } else if (pConstraint instanceof TypeConstraint) {
            createCheck((TypeConstraint) pConstraint,variableMapping);
        }  else {
            String msg = "Unsupported Check constraint: "+pConstraint.toString();
            throw new QueryProcessingException(msg, null, msg, null);
        }

    }

    protected void createExtendDispatcher(PConstraint pConstraint, Map<PVariable, Integer> variableMapping) {

        // DeferredPConstraint subclasses

        // Equalities are normalized
        if (pConstraint instanceof PositivePatternCall) {
            createExtend((PositivePatternCall)pConstraint, variableMapping);
        } else if (pConstraint instanceof AggregatorConstraint) {
            createExtend((AggregatorConstraint) pConstraint, variableMapping);
        } else if (pConstraint instanceof PatternMatchCounter) {
            createExtend((PatternMatchCounter) pConstraint, variableMapping);
        } else if (pConstraint instanceof ExpressionEvaluation) {
            createExtend((ExpressionEvaluation) pConstraint, variableMapping);
        } else if (pConstraint instanceof ExportedParameter) {
            // ExportedParameters are compiled to NOP
        } else

        // EnumerablePConstraint subclasses

        if (pConstraint instanceof ConstantValue) {
            createExtend((ConstantValue) pConstraint, variableMapping);
        } else if (pConstraint instanceof TypeConstraint) {
            createExtend((TypeConstraint) pConstraint, variableMapping);
        } else if (pConstraint instanceof BinaryTransitiveClosure) {
            createExtend((BinaryTransitiveClosure)pConstraint, variableMapping);
        } else if (pConstraint instanceof BinaryReflexiveTransitiveClosure) {
            createExtend((BinaryReflexiveTransitiveClosure)pConstraint, variableMapping);
        } else {
            String msg = "Unsupported Extend constraint: "+pConstraint.toString();
            throw new QueryProcessingException(msg, null, msg, null);
        }
    }

    private boolean isCheck(PConstraint pConstraint, final Map<PVariable, Integer> variableMapping) {
        if (pConstraint instanceof NegativePatternCall){
            return true;
        }else if (pConstraint instanceof PositivePatternCall){
            // Positive pattern call is check if all non-single used variables are bound
            List<Integer> callVariables = pConstraint.getAffectedVariables().stream()
                .filter(input -> input.getReferringConstraints().size() > 1)
                .map(variableMapping::get)
                .collect(Collectors.toList());
            return variableBindings.get(pConstraint).containsAll(callVariables);
        }else if (pConstraint instanceof AggregatorConstraint){
            PVariable outputvar = ((AggregatorConstraint) pConstraint).getResultVariable();
            return variableBindings.get(pConstraint).contains(variableMapping.get(outputvar));
        }else if (pConstraint instanceof PatternMatchCounter){
            PVariable outputvar = ((PatternMatchCounter) pConstraint).getResultVariable();
            return variableBindings.get(pConstraint).contains(variableMapping.get(outputvar));
        }else if (pConstraint instanceof ExpressionEvaluation){
            PVariable outputvar = ((ExpressionEvaluation) pConstraint).getOutputVariable();
            return outputvar == null || variableBindings.get(pConstraint).contains(variableMapping.get(outputvar));
        } else {
            // In other cases, all variables shall be bound to be a check
            Set<PVariable> affectedVariables = pConstraint.getAffectedVariables();
            Set<Integer> varIndices = new HashSet<>();
            for (PVariable variable : affectedVariables) {
                varIndices.add(variableMapping.get(variable));
            }
            return variableBindings.get(pConstraint).containsAll(varIndices);
        }
    }

    @Override
    public Set<CallWithAdornment> getDependencies() {
        return dependencies;
    }

    /**
     * @return the cached variable bindings for the previously created plan
     */
    @Override
    public Map<PVariable, Integer> getVariableMappings() {
        return variableMappings;
    }

    protected void createCheck(PatternMatchCounter counter, Map<PVariable, Integer> variableMapping) {
        CallInformation information = CallInformation.create(counter, variableMapping, variableBindings.get(counter));
        operations.add(new CountCheck(information, variableMapping.get(counter.getResultVariable())));
        dependencies.add(information.getCallWithAdornment());
    }

    protected void createCheck(PositivePatternCall pCall, Map<PVariable, Integer> variableMapping) {
        CallInformation information = CallInformation.create(pCall, variableMapping, variableBindings.get(pCall));
        operations.add(new CheckPositivePatternCall(information));
        dependencies.add(information.getCallWithAdornment());
    }

    protected void createCheck(ConstantValue constant, Map<PVariable, Integer> variableMapping) {
        int position = variableMapping.get(constant.getVariablesTuple().get(0));
        operations.add(new CheckConstant(position, constant.getSupplierKey()));
    }

    protected void createCheck(BinaryTransitiveClosure binaryTransitiveClosure, Map<PVariable, Integer> variableMapping) {
        int sourcePosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(0));
        int targetPosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(1));

        //The second parameter is NOT bound during execution!
        CallInformation information = CallInformation.create(binaryTransitiveClosure, variableMapping, Stream.of(sourcePosition).collect(Collectors.toSet()));
        operations.add(new BinaryTransitiveClosureCheck(information, sourcePosition, targetPosition, false));
        dependencies.add(information.getCallWithAdornment());
    }

    /**
     * @since 2.0
     */
    protected void createCheck(BinaryReflexiveTransitiveClosure binaryTransitiveClosure, Map<PVariable, Integer> variableMapping) {
        int sourcePosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(0));
        int targetPosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(1));

        //The second parameter is NOT bound during execution!
        CallInformation information = CallInformation.create(binaryTransitiveClosure, variableMapping, Stream.of(sourcePosition).collect(Collectors.toSet()));
        createUnaryTypeCheck(binaryTransitiveClosure.getUniverseType(), sourcePosition);
        operations.add(new BinaryTransitiveClosureCheck(information, sourcePosition, targetPosition, true));
        dependencies.add(information.getCallWithAdornment());
    }

    protected void createCheck(ExpressionEvaluation expressionEvaluation, Map<PVariable, Integer> variableMapping) {
        // Fill unbound variables with null; simply copy all variables. Unbound variables will be null anyway
        Iterable<String> inputParameterNames = expressionEvaluation.getEvaluator().getInputParameterNames();
        Map<String, Integer> nameMap = new HashMap<>();

        for (String pVariableName : inputParameterNames) {
            PVariable pVariable = expressionEvaluation.getPSystem().getVariableByNameChecked(pVariableName);
            nameMap.put(pVariableName, variableMapping.get(pVariable));
        }

        // output variable can be null; if null it is an ExpressionCheck
        if(expressionEvaluation.getOutputVariable() == null){
            operations.add(new ExpressionCheck(expressionEvaluation.getEvaluator(), nameMap));
        } else {
            operations.add(new ExpressionEvalCheck(expressionEvaluation.getEvaluator(), nameMap, expressionEvaluation.isUnwinding(), variableMapping.get(expressionEvaluation.getOutputVariable())));
        }
    }

    protected void createCheck(AggregatorConstraint aggregator, Map<PVariable, Integer> variableMapping) {
        CallInformation information = CallInformation.create(aggregator, variableMapping, variableBindings.get(aggregator));
        operations.add(new AggregatorCheck(information, aggregator, variableMapping.get(aggregator.getResultVariable())));
        dependencies.add(information.getCallWithAdornment());
    }

    protected void createCheck(NegativePatternCall negativePatternCall, Map<PVariable, Integer> variableMapping) {
        CallInformation information = CallInformation.create(negativePatternCall, variableMapping, variableBindings.get(negativePatternCall));
        operations.add(new NACOperation(information));
        dependencies.add(information.getCallWithAdornment());
    }

    protected void createCheck(Inequality inequality, Map<PVariable, Integer> variableMapping) {
        operations.add(new InequalityCheck(variableMapping.get(inequality.getWho()), variableMapping.get(inequality.getWithWhom())));
    }

    protected void createExtend(PositivePatternCall pCall, Map<PVariable, Integer> variableMapping) {
        CallInformation information = CallInformation.create(pCall, variableMapping, variableBindings.get(pCall));
        operations.add(new ExtendPositivePatternCall(information));
        dependencies.add(information.getCallWithAdornment());
    }

    protected void createExtend(BinaryTransitiveClosure binaryTransitiveClosure, Map<PVariable, Integer> variableMapping) {
        int sourcePosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(0));
        int targetPosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(1));

        boolean sourceBound = variableBindings.get(binaryTransitiveClosure).contains(sourcePosition);
        boolean targetBound = variableBindings.get(binaryTransitiveClosure).contains(targetPosition);

        CallInformation information = CallInformation.create(binaryTransitiveClosure, variableMapping, variableBindings.get(binaryTransitiveClosure));

        if (sourceBound && !targetBound) {
            operations.add(new ExtendBinaryTransitiveClosure.Forward(information, sourcePosition, targetPosition, false));
            dependencies.add(information.getCallWithAdornment());
        } else if (!sourceBound && targetBound) {
            operations.add(new ExtendBinaryTransitiveClosure.Backward(information, sourcePosition, targetPosition, false));
            dependencies.add(information.getCallWithAdornment());
        } else {
            String msg = "Binary transitive closure not supported with two unbound parameters";
            throw new QueryProcessingException(msg, null, msg, binaryTransitiveClosure.getPSystem().getPattern());
        }
    }

    /**
     * @since 2.0
     */
    protected void createExtend(BinaryReflexiveTransitiveClosure binaryTransitiveClosure, Map<PVariable, Integer> variableMapping) {
        int sourcePosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(0));
        int targetPosition = variableMapping.get(binaryTransitiveClosure.getVariablesTuple().get(1));

        boolean sourceBound = variableBindings.get(binaryTransitiveClosure).contains(sourcePosition);
        boolean targetBound = variableBindings.get(binaryTransitiveClosure).contains(targetPosition);

        CallInformation information = CallInformation.create(binaryTransitiveClosure, variableMapping, variableBindings.get(binaryTransitiveClosure));

        if (sourceBound && !targetBound) {
            createUnaryTypeCheck(binaryTransitiveClosure.getUniverseType(), sourcePosition);
            operations.add(new ExtendBinaryTransitiveClosure.Forward(information, sourcePosition, targetPosition, true));
            dependencies.add(information.getCallWithAdornment());
        } else if (!sourceBound && targetBound) {
            createUnaryTypeCheck(binaryTransitiveClosure.getUniverseType(), targetPosition);
            operations.add(new ExtendBinaryTransitiveClosure.Backward(information, sourcePosition, targetPosition, true));
            dependencies.add(information.getCallWithAdornment());
        } else {
            String msg = "Binary reflective transitive closure not supported with two unbound parameters";
            throw new QueryProcessingException(msg, null, msg, binaryTransitiveClosure.getPSystem().getPattern());
        }
    }

    protected void createExtend(ConstantValue constant, Map<PVariable, Integer> variableMapping) {
        int position = variableMapping.get(constant.getVariablesTuple().get(0));
        operations.add(new ExtendConstant(position, constant.getSupplierKey()));
    }

    protected void createExtend(ExpressionEvaluation expressionEvaluation, Map<PVariable, Integer> variableMapping) {
        // Fill unbound variables with null; simply copy all variables. Unbound variables will be null anyway
        Iterable<String> inputParameterNames = expressionEvaluation.getEvaluator().getInputParameterNames();
        Map<String, Integer> nameMap = new HashMap<>();

        for (String pVariableName : inputParameterNames) {
            PVariable pVariable = expressionEvaluation.getPSystem().getVariableByNameChecked(pVariableName);
            nameMap.put(pVariableName, variableMapping.get(pVariable));
        }

        // output variable can be null; if null it is an ExpressionCheck
        if(expressionEvaluation.getOutputVariable() == null){
            operations.add(new ExpressionCheck(expressionEvaluation.getEvaluator(), nameMap));
        } else {
            operations.add(new ExpressionEval(expressionEvaluation.getEvaluator(), nameMap, expressionEvaluation.isUnwinding(), variableMapping.get(expressionEvaluation.getOutputVariable())));
        }
    }

    protected void createExtend(AggregatorConstraint aggregator, Map<PVariable, Integer> variableMapping) {
        CallInformation information = CallInformation.create(aggregator, variableMapping, variableBindings.get(aggregator));
        operations.add(new AggregatorExtend(information, aggregator, variableMapping.get(aggregator.getResultVariable())));
        dependencies.add(information.getCallWithAdornment());
    }

    protected void createExtend(PatternMatchCounter patternMatchCounter, Map<PVariable, Integer> variableMapping) {
        CallInformation information = CallInformation.create(patternMatchCounter, variableMapping, variableBindings.get(patternMatchCounter));
        operations.add(new CountOperation(information, variableMapping.get(patternMatchCounter.getResultVariable())));
        dependencies.add(information.getCallWithAdornment());
    }

}
