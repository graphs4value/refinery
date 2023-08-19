/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.localsearch.planner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.base.api.BaseIndexOptions;
import tools.refinery.viatra.runtime.base.comprehension.EMFModelComprehension;
import tools.refinery.viatra.runtime.emf.types.EStructuralFeatureInstancesKey;
import tools.refinery.viatra.runtime.localsearch.planner.cost.IConstraintEvaluationContext;
import tools.refinery.viatra.runtime.matchers.backend.ResultProviderRequestor;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryBackendContext;
import tools.refinery.viatra.runtime.matchers.psystem.PConstraint;
import tools.refinery.viatra.runtime.matchers.psystem.PVariable;
import tools.refinery.viatra.runtime.matchers.psystem.basicdeferred.AggregatorConstraint;
import tools.refinery.viatra.runtime.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.viatra.runtime.matchers.psystem.basicdeferred.ExpressionEvaluation;
import tools.refinery.viatra.runtime.matchers.psystem.basicdeferred.Inequality;
import tools.refinery.viatra.runtime.matchers.psystem.basicdeferred.PatternMatchCounter;
import tools.refinery.viatra.runtime.matchers.psystem.basicdeferred.TypeFilterConstraint;
import tools.refinery.viatra.runtime.matchers.psystem.basicenumerables.AbstractTransitiveClosure;
import tools.refinery.viatra.runtime.matchers.psystem.basicenumerables.ConstantValue;
import tools.refinery.viatra.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import tools.refinery.viatra.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PParameter;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PParameterDirection;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.util.Sets;


/**
 * @author Grill Balázs
 * @noreference This class is not intended to be referenced by clients.
 */
class PConstraintInfoInferrer {
    
    private static final Predicate<PVariable> SINGLE_USE_VARIABLE = input -> input != null && input.getReferringConstraints().size() == 1;

    private final boolean useIndex;
    private final Function<IConstraintEvaluationContext, Double> costFunction;
    private final EMFModelComprehension modelComprehension;
    private final IQueryBackendContext context;
    private final ResultProviderRequestor resultRequestor;

    
    public PConstraintInfoInferrer(boolean useIndex, 
            IQueryBackendContext backendContext, 
            ResultProviderRequestor resultRequestor,
            Function<IConstraintEvaluationContext, Double> costFunction) {
        this.useIndex = useIndex;
        this.context = backendContext;
        this.resultRequestor = resultRequestor;
        this.costFunction = costFunction;
        this.modelComprehension = new EMFModelComprehension(new BaseIndexOptions());
    }
    
    
    /**
     * Create all possible application condition for all constraint
     * 
     * @param constraintSet the set of constraints
     * @param runtimeContext the model dependent runtime contest
     * @return a collection of the wrapper PConstraintInfo objects with all the allowed application conditions
     */
    public List<PConstraintInfo> createPConstraintInfos(Set<PConstraint> constraintSet) {
        List<PConstraintInfo> constraintInfos = new ArrayList<>();

        for (PConstraint pConstraint : constraintSet) {
            createPConstraintInfoDispatch(constraintInfos, pConstraint);
        }
        return constraintInfos;
    }

    private void createPConstraintInfoDispatch(List<PConstraintInfo> resultList, PConstraint pConstraint){
        if(pConstraint instanceof ExportedParameter){
            createConstraintInfoExportedParameter(resultList, (ExportedParameter) pConstraint);
        } else if(pConstraint instanceof TypeConstraint){
            createConstraintInfoTypeConstraint(resultList, (TypeConstraint)pConstraint);
        } else if(pConstraint instanceof TypeFilterConstraint){
            createConstraintInfoTypeFilterConstraint(resultList, (TypeFilterConstraint)pConstraint);
        } else if(pConstraint instanceof ConstantValue){
            createConstraintInfoConstantValue(resultList, (ConstantValue)pConstraint);
        } else if (pConstraint instanceof Inequality){
            createConstraintInfoInequality(resultList, (Inequality) pConstraint);
        } else if (pConstraint instanceof ExpressionEvaluation){
            createConstraintInfoExpressionEvaluation(resultList, (ExpressionEvaluation)pConstraint);
        } else if (pConstraint instanceof AggregatorConstraint){
            createConstraintInfoAggregatorConstraint(resultList, pConstraint, ((AggregatorConstraint) pConstraint).getResultVariable());
        } else if (pConstraint instanceof PatternMatchCounter){
            createConstraintInfoAggregatorConstraint(resultList, pConstraint, ((PatternMatchCounter) pConstraint).getResultVariable());   
        } else if (pConstraint instanceof PositivePatternCall){
            createConstraintInfoPositivePatternCall(resultList, (PositivePatternCall) pConstraint);
        } else if (pConstraint instanceof AbstractTransitiveClosure) {
            createConstraintInfoBinaryTransitiveClosure(resultList, (AbstractTransitiveClosure) pConstraint);
        } else{
            createConstraintInfoGeneric(resultList, pConstraint);
        }
    }
    
    private void createConstraintInfoConstantValue(List<PConstraintInfo> resultList, 
            ConstantValue pConstraint) {
        // A ConstantValue constraint has a single variable, which is allowed to be unbound 
        // (extending through ConstantValue is considered a cheap operation)
        Set<PVariable> affectedVariables = pConstraint.getAffectedVariables();
        Set<? extends Set<PVariable>> bindings = Sets.powerSet(affectedVariables);
        doCreateConstraintInfos(resultList, pConstraint, affectedVariables, bindings);
    }


    private void createConstraintInfoPositivePatternCall(List<PConstraintInfo> resultList, 
            PositivePatternCall pCall) {
        // A pattern call can have any of its variables unbound
        Set<PVariable> affectedVariables = pCall.getAffectedVariables();
        // IN parameters cannot be unbound and
        // OUT parameters cannot be bound
        Tuple variables = pCall.getVariablesTuple();
        final Set<PVariable> inVariables = new HashSet<>();
        Set<PVariable> inoutVariables = new HashSet<>();
        List<PParameter> parameters = pCall.getReferredQuery().getParameters();
        for(int i=0;i<parameters.size();i++){
            switch(parameters.get(i).getDirection()){
            case IN:
                inVariables.add((PVariable) variables.get(i));
                break;
            case INOUT:
                inoutVariables.add((PVariable) variables.get(i));
                break;
            case OUT:
            default:
                break;
            
            }
        }
        Iterable<Set<PVariable>> bindings = Sets.powerSet(inoutVariables).stream()
                .map(input -> Stream.concat(input.stream(), inVariables.stream()).collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        
        doCreateConstraintInfos(resultList, pCall, affectedVariables, bindings);
    }
    
    private void createConstraintInfoBinaryTransitiveClosure(List<PConstraintInfo> resultList, 
            AbstractTransitiveClosure closure) {
        // A pattern call can have any of its variables unbound
        
        List<PParameter> parameters = closure.getReferredQuery().getParameters();
        Tuple variables = closure.getVariablesTuple();
        
        Set<Set<PVariable>> bindings = new HashSet<>();
        PVariable firstVariable = (PVariable) variables.get(0);
        PVariable secondVariable = (PVariable) variables.get(1);
        // Check is always supported
        bindings.add(new HashSet<>(Arrays.asList(firstVariable, secondVariable)));
        // If first parameter is not bound mandatorily, it can be left out
        if (parameters.get(0).getDirection() != PParameterDirection.IN) {
            bindings.add(Collections.singleton(secondVariable));
        }
        // If second parameter is not bound mandatorily, it can be left out
        if (parameters.get(1).getDirection() != PParameterDirection.IN) {
            bindings.add(Collections.singleton(firstVariable));
        }
        
        doCreateConstraintInfos(resultList, closure, closure.getAffectedVariables(), bindings);
    }
    
    

    private void createConstraintInfoExportedParameter(List<PConstraintInfo> resultList, 
            ExportedParameter parameter) {
        // In case of an exported parameter constraint, the parameter must be bound in order to execute
        Set<PVariable> affectedVariables = parameter.getAffectedVariables();
        doCreateConstraintInfos(resultList, parameter, affectedVariables, Collections.singleton(affectedVariables));
    }
    
    private void createConstraintInfoExpressionEvaluation(List<PConstraintInfo> resultList, 
            ExpressionEvaluation expressionEvaluation) {
        // An expression evaluation can only have its output variable unbound. All other variables shall be bound
        PVariable output = expressionEvaluation.getOutputVariable();
        Set<Set<PVariable>> bindings = new HashSet<>();
        Set<PVariable> affectedVariables = expressionEvaluation.getAffectedVariables();
        // All variables bound -> check
        bindings.add(affectedVariables);
        // Output variable is not bound -> extend
        bindings.add(affectedVariables.stream().filter(var -> !Objects.equals(var, output)).collect(Collectors.toSet()));
        doCreateConstraintInfos(resultList, expressionEvaluation, affectedVariables, bindings);
    }

    private void createConstraintInfoTypeFilterConstraint(List<PConstraintInfo> resultList, 
            TypeFilterConstraint filter){
        // In case of type filter, all affected variables must be bound in order to execute
        Set<PVariable> affectedVariables = filter.getAffectedVariables();
        doCreateConstraintInfos(resultList, filter, affectedVariables, Collections.singleton(affectedVariables));
    }
    
    private void createConstraintInfoInequality(List<PConstraintInfo> resultList, 
            Inequality inequality){
        // In case of inequality, all affected variables must be bound in order to execute
        Set<PVariable> affectedVariables = inequality.getAffectedVariables();
        doCreateConstraintInfos(resultList, inequality, affectedVariables, Collections.singleton(affectedVariables));
    }
    
    private void createConstraintInfoAggregatorConstraint(List<PConstraintInfo> resultList, 
            PConstraint pConstraint, PVariable resultVariable){
        Set<PVariable> affectedVariables = pConstraint.getAffectedVariables();
        
        // The only variables which can be unbound are single-use
        Set<PVariable> canBeUnboundVariables = 
                Stream.concat(Stream.of(resultVariable), affectedVariables.stream().filter(SINGLE_USE_VARIABLE)).collect(Collectors.toSet());
       
        Set<Set<PVariable>> bindings = calculatePossibleBindings(canBeUnboundVariables, affectedVariables);
        
        doCreateConstraintInfos(resultList, pConstraint, affectedVariables, bindings);
    }
    
    /**
     * 
     * @param canBeUnboundVariables Variables which are allowed to be unbound
     * @param affectedVariables All affected variables
     * @return The set of possible bound variable sets
     */
    private Set<Set<PVariable>> calculatePossibleBindings(Set<PVariable> canBeUnboundVariables, Set<PVariable> affectedVariables){
        final Set<PVariable> mustBindVariables = affectedVariables.stream().filter(input -> !canBeUnboundVariables.contains(input)).collect(Collectors.toSet()); 
        return Sets.powerSet(canBeUnboundVariables).stream()
                .map(input -> {
                    //some variables have to be bound before executing this constraint
                    Set<PVariable> result= new HashSet<>(input);
                    result.addAll(mustBindVariables);
                    return result;
                })
                .collect(Collectors.toSet());
    }
    
    private void createConstraintInfoGeneric(List<PConstraintInfo> resultList, PConstraint pConstraint){
        Set<PVariable> affectedVariables = pConstraint.getAffectedVariables();
        
        // The only variables which can be unbound are single use variables
        Set<PVariable> canBeUnboundVariables = affectedVariables.stream().filter(SINGLE_USE_VARIABLE).collect(Collectors.toSet());
       
        Set<Set<PVariable>> bindings = calculatePossibleBindings(canBeUnboundVariables, affectedVariables);
        
        doCreateConstraintInfos(resultList, pConstraint, affectedVariables, bindings);
    }
    
    private boolean canPerformInverseNavigation(EStructuralFeature feature){
        return ( // Feature has opposite (this only possible for references)
                 hasEOpposite(feature) 
                 ||
                 (feature instanceof EReference) && ((EReference)feature).isContainment()
                 || (   // Indexing is enabled, and the feature can be indexed (not a non-well-behaving derived feature).
                        useIndex && modelComprehension.representable(feature)
                 ));
    }
    
    private void createConstraintInfoTypeConstraint(List<PConstraintInfo> resultList, 
            TypeConstraint typeConstraint) {
        Set<PVariable> affectedVariables = typeConstraint.getAffectedVariables();
        Set<? extends Set<PVariable>> bindings = null;
        
        IInputKey inputKey = typeConstraint.getSupplierKey();
        if(inputKey.isEnumerable()){
            bindings = Sets.powerSet(affectedVariables);
        }else{
            // For not enumerable types, this constraint can only be a check
            bindings = Collections.singleton(affectedVariables);
        }
        
        if(inputKey instanceof EStructuralFeatureInstancesKey){
            final EStructuralFeature feature = ((EStructuralFeatureInstancesKey) inputKey).getEmfKey();
            if(!canPerformInverseNavigation(feature)){
                // When inverse navigation is not allowed or not possible, filter out operation masks, where
                // the first variable would be free AND the feature is an EReference and has no EOpposite
                bindings = excludeUnnavigableOperationMasks(typeConstraint, bindings);
            }
        }
        doCreateConstraintInfos(resultList, typeConstraint, affectedVariables, bindings);
    }
    
    private void doCreateConstraintInfos(List<PConstraintInfo> constraintInfos,
            PConstraint pConstraint, Set<PVariable> affectedVariables, Iterable<? extends Set<PVariable>> bindings) {
        Set<PConstraintInfo> sameWithDifferentBindings = new HashSet<>();
        for (Set<PVariable> boundVariables : bindings) {
            
            PConstraintInfo info = new PConstraintInfo(pConstraint, boundVariables,
                    affectedVariables.stream().filter(input -> !boundVariables.contains(input)).collect(Collectors.toSet()),
                    sameWithDifferentBindings, context, resultRequestor, costFunction);
            constraintInfos.add(info);
            sameWithDifferentBindings.add(info);
        }
    }
    
    private Set<Set<PVariable>> excludeUnnavigableOperationMasks(TypeConstraint typeConstraint, Set<? extends Set<PVariable>> bindings) {
        PVariable firstVariable = typeConstraint.getVariableInTuple(0);
        return bindings.stream().filter(
                boundVariablesSet -> (boundVariablesSet.isEmpty() || boundVariablesSet.contains(firstVariable)))
                .collect(Collectors.toSet());
    }
    
    private boolean hasEOpposite(EStructuralFeature feature) {
        if(feature instanceof EReference){
            EReference eOpposite = ((EReference) feature).getEOpposite();
            if(eOpposite != null){
                return true;
            }
        }
        return false;
    }
    
}
