/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.planner.compiler.IOperationCompiler;
import tools.refinery.interpreter.localsearch.matcher.integration.LocalSearchHints;
import tools.refinery.interpreter.localsearch.plan.SearchPlanForBody;
import tools.refinery.interpreter.matchers.backend.ResultProviderRequestor;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.rewriters.PBodyNormalizer;
import tools.refinery.interpreter.matchers.psystem.rewriters.PDisjunctionRewriter;
import tools.refinery.interpreter.matchers.psystem.rewriters.PDisjunctionRewriterCacher;
import tools.refinery.interpreter.matchers.psystem.rewriters.PQueryFlattener;

/**
 *
 * @author Marton Bur
 * @noreference This class is not intended to be referenced by clients.
 */
public class LocalSearchPlanner implements ILocalSearchPlanner {

    // Externally set tools for planning
    private final PDisjunctionRewriter preprocessor;
    private final LocalSearchRuntimeBasedStrategy plannerStrategy;
    private final IQueryRuntimeContext runtimeContext;
    private final LocalSearchHints configuration;
    private final IOperationCompiler operationCompiler;
    private final IQueryBackendContext context;
    private final ResultProviderRequestor resultRequestor;

    /**
     * @param resultRequestor
     * @since 1.7
     */
    public LocalSearchPlanner(IQueryBackendContext backendContext, IOperationCompiler compiler, Logger logger,
            final LocalSearchHints configuration, ResultProviderRequestor resultRequestor)
    {

        this.runtimeContext = backendContext.getRuntimeContext();
        this.configuration = configuration;
        this.operationCompiler = compiler;
        this.resultRequestor = resultRequestor;
        PQueryFlattener flattener = new PQueryFlattener(configuration.getFlattenCallPredicate());
        /*
         * TODO https://bugs.eclipse.org/bugs/show_bug.cgi?id=439358: The normalizer is initialized with the false
         * parameter to turn off unary constraint elimination to work around an issue related to plan ordering: the
         * current implementation of the feature target checking operations expect that the source types were checked
         * before. However, this causes duplicate constraint checks in the search plan that might affect performance
         * negatively.
         */
        PBodyNormalizer normalizer = new PBodyNormalizer(runtimeContext.getMetaContext()) {

            @Override
            protected boolean shouldCalculateImpliedTypes(PQuery query) {
                return false;
            }
        };
        preprocessor = new PDisjunctionRewriterCacher(flattener, normalizer);

        plannerStrategy = new LocalSearchRuntimeBasedStrategy();

        context = backendContext;
    }

    /**
     * Creates executable plans for the provided query. It is required to call one of the
     * <code>initializePlanner()</code> methods before calling this method.
     *
     * @param querySpec
     * @param boundParameters
     *            a set of bound parameters
     * @return a mapping between ISearchOperation list and a mapping, that holds a PVariable-Integer mapping for the
     *         list of ISearchOperations
     */
    @Override
    public Collection<SearchPlanForBody> plan(PQuery querySpec, Set<PParameter> boundParameters) {
        // 1. Preparation
        preprocessor.setTraceCollector(configuration.getTraceCollector());
        Set<PBody> normalizedBodies = preprocessor.rewrite(querySpec.getDisjunctBodies()).getBodies();

        List<SearchPlanForBody> plansForBodies = new ArrayList<>(normalizedBodies.size());

        for (PBody normalizedBody : normalizedBodies) {
            // 2. Plan creation
            // Context has matchers for the referred Queries (IQuerySpecifications)
            Set<PVariable> boundVariables = calculatePatternAdornmentForPlanner(boundParameters, normalizedBody);
            PlanState searchPlanInternal = plannerStrategy.plan(normalizedBody, boundVariables, context, resultRequestor, configuration);
            SubPlan plan = plannerStrategy.convertPlan(boundVariables, searchPlanInternal);
            // 3. PConstraint -> POperation compilation step
            // * Pay extra caution to extend operations, when more than one variables are unbound
            List<ISearchOperation> compiledOperations = operationCompiler.compile(plan, boundParameters);
            // Store the variable mappings for the plans for debug purposes (traceability information)
            SearchPlanForBody compiledPlan = new SearchPlanForBody(normalizedBody,
                    operationCompiler.getVariableMappings(), plan, compiledOperations,
                    operationCompiler.getDependencies(),
                    searchPlanInternal, searchPlanInternal.getCost());

            plansForBodies.add(compiledPlan);
        }

        return plansForBodies;
    }

    private Set<PVariable> calculatePatternAdornmentForPlanner(Set<PParameter> boundParameters, PBody normalizedBody) {
        Map<PParameter, PVariable> parameterMapping = new HashMap<>();
        for (ExportedParameter constraint : normalizedBody.getSymbolicParameters()) {
            parameterMapping.put(constraint.getPatternParameter(), constraint.getParameterVariable());
        }
        Set<PVariable> boundVariables = new HashSet<>();
        for (PParameter parameter : boundParameters) {
            PVariable mappedParameter = parameterMapping.get(parameter);
            boundVariables.add(mappedParameter);
        }
        return boundVariables;
    }

}
