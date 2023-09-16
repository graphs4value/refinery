/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.construction.quasitree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.backend.IQueryBackendHintProvider;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.planning.IQueryPlannerStrategy;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.SubPlanFactory;
import tools.refinery.interpreter.matchers.planning.helpers.BuildHelper;
import tools.refinery.interpreter.matchers.planning.operations.PApply;
import tools.refinery.interpreter.matchers.planning.operations.PEnumerate;
import tools.refinery.interpreter.matchers.planning.operations.PProject;
import tools.refinery.interpreter.matchers.planning.operations.PStart;
import tools.refinery.interpreter.matchers.psystem.DeferredPConstraint;
import tools.refinery.interpreter.matchers.psystem.EnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.ConstantValue;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.rete.construction.RetePatternBuildException;
import tools.refinery.interpreter.rete.util.ReteHintOptions;

/**
 * Layout ideas: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=398763
 *
 * @author Gabor Bergmann
 *
 */
public class QuasiTreeLayout implements IQueryPlannerStrategy {

    private IQueryBackendHintProvider hintProvider;
    private IQueryBackendContext backendContext;
    private QueryAnalyzer queryAnalyzer;

    public QuasiTreeLayout(IQueryBackendContext backendContext) {
        this(backendContext, backendContext.getHintProvider());
    }

    public QuasiTreeLayout(IQueryBackendContext backendContext, IQueryBackendHintProvider hintProvider) {
        this.backendContext = backendContext;
        this.hintProvider = hintProvider;
        queryAnalyzer = backendContext.getQueryAnalyzer();
    }

    @Override
    public SubPlan plan(PBody pSystem, Logger logger, IQueryMetaContext context) {
        return new Scaffold(pSystem, logger, context).run();
    }

    public class Scaffold {
        PBody pSystem;
        PQuery query;
        IQueryMetaContext context;
        private QueryEvaluationHint hints;
        //IOperationCompiler compiler;
        //SubPlanProcessor planProcessor = new SubPlanProcessor();
        SubPlanFactory planFactory;

        Set<DeferredPConstraint> deferredConstraints = null;
        Set<EnumerablePConstraint> enumerableConstraints = null;
        Set<ConstantValue> constantConstraints = null;
        Set<SubPlan> forefront = new LinkedHashSet<SubPlan>();
        Logger logger;

        Scaffold(PBody pSystem, Logger logger, /*IOperationCompiler compiler,*/ IQueryMetaContext context) {
            this.pSystem = pSystem;
            this.logger = logger;
            this.context = context;
            this.planFactory = new SubPlanFactory(pSystem);
            query = pSystem.getPattern();
            //this.compiler = compiler;
            //planProcessor.setCompiler(compiler);

            hints = hintProvider.getQueryEvaluationHint(query);
        }

        /**
         * @throws InterpreterRuntimeException
         */
        public SubPlan run() {
            try {
                logger.debug(String.format(
                        "%s: patternbody build started for %s",
                        getClass().getSimpleName(),
                        query.getFullyQualifiedName()));

                // PROCESS CONSTRAINTS
                deferredConstraints = pSystem.getConstraintsOfType(DeferredPConstraint.class);
                enumerableConstraints = pSystem.getConstraintsOfType(EnumerablePConstraint.class);
                constantConstraints = pSystem.getConstraintsOfType(ConstantValue.class);

                for (EnumerablePConstraint enumerable : enumerableConstraints) {
                    SubPlan plan = planFactory.createSubPlan(new PEnumerate(enumerable));
                    admitSubPlan(plan);
                }
                if (enumerableConstraints.isEmpty()) { // EXTREME CASE
                    SubPlan plan = planFactory.createSubPlan(new PStart());
                    admitSubPlan(plan);
                }

                // JOIN FOREFRONT PLANS WHILE POSSIBLE
                while (forefront.size() > 1) {
                    // TODO QUASI-TREE TRIVIAL JOINS?

                    List<JoinCandidate> candidates = generateJoinCandidates();
                    JoinOrderingHeuristics ordering = new JoinOrderingHeuristics();
                    JoinCandidate selectedJoin = Collections.min(candidates, ordering);
                    doJoin(selectedJoin);
                }
                assert (forefront.size() == 1);

                // PROJECT TO PARAMETERS
                SubPlan preFinalPlan = forefront.iterator().next();
                SubPlan finalPlan = planFactory.createSubPlan(new PProject(pSystem.getSymbolicParameterVariables()), preFinalPlan);

                // FINAL CHECK, whether all exported variables are present + all constraint satisfied
                BuildHelper.finalCheck(pSystem, finalPlan, context);
                // TODO integrate the check above in SubPlan / POperation

                logger.debug(String.format(
                        "%s: patternbody query plan concluded for %s as: %s",
                        getClass().getSimpleName(),
                        query.getFullyQualifiedName(),
                        finalPlan.toLongString()));
               return finalPlan;
            } catch (RetePatternBuildException ex) {
                ex.setPatternDescription(query);
                throw ex;
            }
        }

        public List<JoinCandidate> generateJoinCandidates() {
            List<JoinCandidate> candidates = new ArrayList<JoinCandidate>();
            int bIndex = 0;
            for (SubPlan b : forefront) {
                int aIndex = 0;
                for (SubPlan a : forefront) {
                    if (aIndex++ >= bIndex)
                        break;
                    candidates.add(new JoinCandidate(a, b, queryAnalyzer));
                }
                bIndex++;
            }
            return candidates;
        }

        private void admitSubPlan(SubPlan plan) {
            // are there any unapplied constant filters that we can apply here?
            if (ReteHintOptions.prioritizeConstantFiltering.getValueOrDefault(hints)) {
                for (ConstantValue constantConstraint : constantConstraints) {
                    if (!plan.getAllEnforcedConstraints().contains(constantConstraint) &&
                            plan.getVisibleVariables().containsAll(constantConstraint.getAffectedVariables())) {
                        plan = planFactory.createSubPlan(new PApply(constantConstraint), plan);
                    }
                }
            }
            // are there any variables that will not be needed anymore and are worth trimming?
            // (check only if there are unenforced enumerables, so that there are still upcoming joins)
//            if (Options.planTrimOption != Options.PlanTrimOption.OFF &&
//                    !plan.getAllEnforcedConstraints().containsAll(enumerableConstraints)) {
            if (true) {
                final SubPlan trimmed = BuildHelper.trimUnneccessaryVariables(
                        planFactory, plan, true, queryAnalyzer);
                plan = trimmed;
            }
            // are there any checkable constraints?
            for (DeferredPConstraint deferred : deferredConstraints) {
                if (!plan.getAllEnforcedConstraints().contains(deferred)) {
                    if (deferred.isReadyAt(plan, context)) {
                        admitSubPlan(planFactory.createSubPlan(new PApply(deferred), plan));
                        return;
                    }
                }
            }
            // if no checkable constraints and no unused variables
            forefront.add(plan);
        }

        private void doJoin(JoinCandidate selectedJoin) {
            forefront.remove(selectedJoin.getPrimary());
            forefront.remove(selectedJoin.getSecondary());
            admitSubPlan(selectedJoin.getJoinedPlan(planFactory));
        }

    }

}
