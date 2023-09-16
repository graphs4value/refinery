/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.construction.plancompiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import tools.refinery.interpreter.rete.recipes.IndexerBasedAggregatorRecipe;
import tools.refinery.interpreter.rete.recipes.MonotonicityInfo;
import tools.refinery.interpreter.rete.recipes.ProjectionIndexerRecipe;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IPosetComparator;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.planning.helpers.TypeHelper;
import tools.refinery.interpreter.matchers.psystem.EnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration;
import tools.refinery.interpreter.rete.recipes.EqualityFilterRecipe;
import tools.refinery.interpreter.rete.recipes.IndexerRecipe;
import tools.refinery.interpreter.rete.recipes.JoinRecipe;
import tools.refinery.interpreter.rete.recipes.Mask;
import tools.refinery.interpreter.rete.recipes.ProductionRecipe;
import tools.refinery.interpreter.rete.recipes.RecipesFactory;
import tools.refinery.interpreter.rete.recipes.SingleColumnAggregatorRecipe;
import tools.refinery.interpreter.rete.recipes.TrimmerRecipe;
import tools.refinery.interpreter.rete.recipes.helper.RecipesHelper;
import tools.refinery.interpreter.rete.traceability.CompiledQuery;
import tools.refinery.interpreter.rete.traceability.CompiledSubPlan;
import tools.refinery.interpreter.rete.traceability.PlanningTrace;
import tools.refinery.interpreter.rete.traceability.RecipeTraceInfo;
import tools.refinery.interpreter.rete.util.ReteHintOptions;

/**
 * @author Bergmann Gabor
 *
 */
public class CompilerHelper {

    private CompilerHelper() {/*Utility class constructor*/}

    static final RecipesFactory FACTORY = RecipesFactory.eINSTANCE;

    /**
     * Makes sure that all variables in the tuple are different so that it can be used as {@link CompiledSubPlan}. If a
     * variable occurs multiple times, equality checks are applied and then the results are trimmed so that duplicates
     * are hidden. If no manipulation is necessary, the original trace is returned.
     *
     * <p>
     * to be used whenever a constraint introduces new variables.
     */
    public static PlanningTrace checkAndTrimEqualVariables(SubPlan plan, final PlanningTrace coreTrace) {
        // are variables in the constraint all different?
        final List<PVariable> coreVariablesTuple = coreTrace.getVariablesTuple();
        final int constraintArity = coreVariablesTuple.size();
        final int distinctVariables = coreTrace.getPosMapping().size();
        if (constraintArity == distinctVariables) {
            // all variables occur exactly once in tuple
            return coreTrace;
        } else { // apply equality checks and trim

            // find the positions in the tuple for each variable
            Map<PVariable, SortedSet<Integer>> posMultimap = new HashMap<PVariable, SortedSet<Integer>>();
            List<PVariable> trimmedVariablesTuple = new ArrayList<PVariable>(distinctVariables);
            int[] trimIndices = new int[distinctVariables];
            for (int i = 0; i < constraintArity; ++i) {
                final PVariable variable = coreVariablesTuple.get(i);
                SortedSet<Integer> indexSet = posMultimap.get(variable);
                if (indexSet == null) { // first occurrence of variable
                    indexSet = new TreeSet<Integer>();
                    posMultimap.put(variable, indexSet);

                    // this is the first occurrence, set up trimming
                    trimIndices[trimmedVariablesTuple.size()] = i;
                    trimmedVariablesTuple.add(variable);
                }
                indexSet.add(i);
            }

            // construct equality checks for each variable occurring multiple times
            PlanningTrace lastTrace = coreTrace;
            for (Entry<PVariable, SortedSet<Integer>> entry : posMultimap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    EqualityFilterRecipe equalityFilterRecipe = FACTORY.createEqualityFilterRecipe();
                    equalityFilterRecipe.setParent(lastTrace.getRecipe());
                    equalityFilterRecipe.getIndices().addAll(entry.getValue());
                    lastTrace = new PlanningTrace(plan, coreVariablesTuple, equalityFilterRecipe, lastTrace);
                }
            }

            // trim so that each variable occurs only once
            TrimmerRecipe trimmerRecipe = FACTORY.createTrimmerRecipe();
            trimmerRecipe.setParent(lastTrace.getRecipe());
            trimmerRecipe.setMask(RecipesHelper
                    .mask(constraintArity, trimIndices));
            return new PlanningTrace(plan, trimmedVariablesTuple, trimmerRecipe, lastTrace);
        }
    }

    /**
     * Extracts the variable list representation of the variables tuple.
     */
    public static List<PVariable> convertVariablesTuple(EnumerablePConstraint constraint) {
        return convertVariablesTuple(constraint.getVariablesTuple());
    }

    /**
     * Extracts the variable list representation of the variables tuple.
     */
    public static List<PVariable> convertVariablesTuple(Tuple variablesTuple) {
        List<PVariable> result = new ArrayList<PVariable>();
        for (Object o : variablesTuple.getElements())
            result.add((PVariable) o);
        return result;
    }

    /**
     * Returns a compiled indexer trace according to a mask
     */
    public static RecipeTraceInfo makeIndexerTrace(SubPlan planToCompile, PlanningTrace parentTrace, TupleMask mask) {
        final ReteNodeRecipe parentRecipe = parentTrace.getRecipe();
        if (parentRecipe instanceof IndexerBasedAggregatorRecipe
                || parentRecipe instanceof SingleColumnAggregatorRecipe)
            throw new IllegalArgumentException(
                    "Cannot take projection indexer of aggregator node at plan " + planToCompile);
        IndexerRecipe recipe = RecipesHelper.projectionIndexerRecipe(parentRecipe, toRecipeMask(mask));
        // final List<PVariable> maskedVariables = mask.transform(parentTrace.getVariablesTuple());
        return new PlanningTrace(planToCompile, /* maskedVariables */ parentTrace.getVariablesTuple(), recipe,
                parentTrace);
        // TODO add specialized indexer trace info?
    }

    /**
     * Creates a trimmer that keeps selected variables only.
     */
    protected static TrimmerRecipe makeTrimmerRecipe(final PlanningTrace compiledParent,
            List<PVariable> projectedVariables) {
        final Mask projectionMask = makeProjectionMask(compiledParent, projectedVariables);
        final TrimmerRecipe trimmerRecipe = ReteRecipeCompiler.FACTORY.createTrimmerRecipe();
        trimmerRecipe.setParent(compiledParent.getRecipe());
        trimmerRecipe.setMask(projectionMask);
        return trimmerRecipe;
    }

    public static Mask makeProjectionMask(final PlanningTrace compiledParent, Iterable<PVariable> projectedVariables) {
        List<Integer> projectionSourceIndices = new ArrayList<Integer>();
        for (PVariable pVariable : projectedVariables) {
            projectionSourceIndices.add(compiledParent.getPosMapping().get(pVariable));
        }
        final Mask projectionMask = RecipesHelper.mask(compiledParent.getRecipe().getArity(), projectionSourceIndices);
        return projectionMask;
    }

    /**
     * @since 1.6
     */
    public static final class PosetTriplet {
        public Mask coreMask;
        public Mask posetMask;
        public IPosetComparator comparator;
    }

    /**
     * @since 1.6
     */
    public static PosetTriplet computePosetInfo(List<PVariable> variables, PBody body, IQueryMetaContext context) {
        Map<PVariable, Set<IInputKey>> typeMap = TypeHelper.inferUnaryTypesFor(variables, body.getConstraints(),
                context);
        List<Set<IInputKey>> keys = new LinkedList<Set<IInputKey>>();

        for (int i = 0; i < variables.size(); i++) {
            keys.add(typeMap.get(variables.get(i)));
        }

        return computePosetInfo(keys, context);
    }

    /**
     * @since 1.6
     */
    public static PosetTriplet computePosetInfo(List<PParameter> parameters, IQueryMetaContext context) {
        List<Set<IInputKey>> keys = new LinkedList<Set<IInputKey>>();
        for (int i = 0; i < parameters.size(); i++) {
            IInputKey key = parameters.get(i).getDeclaredUnaryType();
            if (key == null) {
                keys.add(Collections.emptySet());
            } else {
                keys.add(Collections.singleton(parameters.get(i).getDeclaredUnaryType()));
            }
        }
        return computePosetInfo(keys, context);
    }



    /**
     * @since 1.6
     */
    public static PosetTriplet computePosetInfo(Iterable<Set<IInputKey>> keys, IQueryMetaContext context) {
        PosetTriplet result = new PosetTriplet();
        List<Integer> coreIndices = new ArrayList<Integer>();
        List<Integer> posetIndices = new ArrayList<Integer>();
        List<IInputKey> filtered = new ArrayList<IInputKey>();
        boolean posetKey = false;
        int index = -1;

        for (Set<IInputKey> _keys : keys) {
            ++index;
            posetKey = false;

            for (IInputKey key : _keys) {
                if (key != null && context.isPosetKey(key)) {
                    posetKey = true;
                    filtered.add(key);
                    break;
                }
            }

            if (posetKey) {
                posetIndices.add(index);
            } else {
                coreIndices.add(index);
            }
        }

        result.comparator = context.getPosetComparator(filtered);
        result.coreMask = RecipesHelper.mask(index + 1, coreIndices);
        result.posetMask = RecipesHelper.mask(index + 1, posetIndices);

        return result;
    }

    /**
     * Creates a recipe for a production node and the corresponding trace.
     * <p> PRE: in case this is a recursion cutoff point (see {@link RecursionCutoffPoint})
     *  and bodyFinalTraces will be filled later,
     *  the object yielded now by bodyFinalTraces.values() must return up-to-date results later
     * @since 2.4
     */
    public static CompiledQuery makeQueryTrace(PQuery query, Map<PBody, RecipeTraceInfo> bodyFinalTraces,
            Collection<ReteNodeRecipe> bodyFinalRecipes, QueryEvaluationHint hint, IQueryMetaContext context,
            boolean deleteAndRederiveEvaluation, TimelyConfiguration timelyEvaluation) {
        ProductionRecipe recipe = ReteRecipeCompiler.FACTORY.createProductionRecipe();

        // temporary solution to support the deprecated option for now
        boolean deleteAndRederiveEvaluationDep = deleteAndRederiveEvaluation || ReteHintOptions.deleteRederiveEvaluation.getValueOrDefault(hint);

        recipe.setDeleteRederiveEvaluation(deleteAndRederiveEvaluationDep);

        if (deleteAndRederiveEvaluationDep || (timelyEvaluation != null)) {
            PosetTriplet triplet = computePosetInfo(query.getParameters(), context);
            if (triplet.comparator != null) {
                MonotonicityInfo info = FACTORY.createMonotonicityInfo();
                info.setCoreMask(triplet.coreMask);
                info.setPosetMask(triplet.posetMask);
                info.setPosetComparator(triplet.comparator);
                recipe.setOptionalMonotonicityInfo(info);
            }
        }

        recipe.setPattern(query);
        recipe.setPatternFQN(query.getFullyQualifiedName());
        recipe.setTraceInfo(recipe.getPatternFQN());
        recipe.getParents().addAll(bodyFinalRecipes);
        for (int i = 0; i < query.getParameterNames().size(); ++i) {
            recipe.getMappedIndices().put(query.getParameterNames().get(i), i);
        }

        return new CompiledQuery(recipe, bodyFinalTraces, query);
    }

    /**
     * Calculated index mappings for a join, based on the common variables of the two parent subplans.
     *
     * @author Gabor Bergmann
     *
     */
    public static class JoinHelper {
        private TupleMask primaryMask;
        private TupleMask secondaryMask;
        private TupleMask complementerMask;
        private RecipeTraceInfo primaryIndexer;
        private RecipeTraceInfo secondaryIndexer;
        private JoinRecipe naturalJoinRecipe;
        private List<PVariable> naturalJoinVariablesTuple;

        /**
         * @pre enforceVariableCoincidences() has been called on both sides.
         */
        public JoinHelper(SubPlan planToCompile, PlanningTrace primaryCompiled, PlanningTrace callTrace) {
            super();

            Set<PVariable> primaryVariables = new LinkedHashSet<PVariable>(primaryCompiled.getVariablesTuple());
            Set<PVariable> secondaryVariables = new LinkedHashSet<PVariable>(callTrace.getVariablesTuple());
            int oldNodes = 0;
            Set<Integer> introducingSecondaryIndices = new TreeSet<Integer>();
            for (PVariable var : secondaryVariables) {
                if (primaryVariables.contains(var))
                    oldNodes++;
                else
                    introducingSecondaryIndices.add(callTrace.getPosMapping().get(var));
            }
            List<Integer> primaryIndices = new ArrayList<Integer>(oldNodes);
            List<Integer> secondaryIndices = new ArrayList<Integer>(oldNodes);
            for (PVariable var : secondaryVariables) {
                if (primaryVariables.contains(var)) {
                    primaryIndices.add(primaryCompiled.getPosMapping().get(var));
                    secondaryIndices.add(callTrace.getPosMapping().get(var));
                }
            }
            Collection<Integer> complementerIndices = introducingSecondaryIndices;

            primaryMask = TupleMask.fromSelectedIndices(primaryCompiled.getVariablesTuple().size(), primaryIndices);
            secondaryMask = TupleMask.fromSelectedIndices(callTrace.getVariablesTuple().size(), secondaryIndices);
            complementerMask = TupleMask.fromSelectedIndices(callTrace.getVariablesTuple().size(), complementerIndices);

            primaryIndexer = makeIndexerTrace(planToCompile, primaryCompiled, primaryMask);
            secondaryIndexer = makeIndexerTrace(planToCompile, callTrace, secondaryMask);

            naturalJoinRecipe = FACTORY.createJoinRecipe();
            naturalJoinRecipe.setLeftParent((ProjectionIndexerRecipe) primaryIndexer.getRecipe());
            naturalJoinRecipe.setRightParent((IndexerRecipe) secondaryIndexer.getRecipe());
            naturalJoinRecipe.setRightParentComplementaryMask(CompilerHelper.toRecipeMask(complementerMask));

            naturalJoinVariablesTuple = new ArrayList<PVariable>(primaryCompiled.getVariablesTuple());
            for (int complementerIndex : complementerMask.indices)
                naturalJoinVariablesTuple.add(callTrace.getVariablesTuple().get(complementerIndex));
        }

        public TupleMask getPrimaryMask() {
            return primaryMask;
        }

        public TupleMask getSecondaryMask() {
            return secondaryMask;
        }

        public TupleMask getComplementerMask() {
            return complementerMask;
        }

        public RecipeTraceInfo getPrimaryIndexer() {
            return primaryIndexer;
        }

        public RecipeTraceInfo getSecondaryIndexer() {
            return secondaryIndexer;
        }

        public JoinRecipe getNaturalJoinRecipe() {
            return naturalJoinRecipe;
        }

        public List<PVariable> getNaturalJoinVariablesTuple() {
            return naturalJoinVariablesTuple;
        }

    }

    /**
     * @since 1.4
     */
    public static Mask toRecipeMask(TupleMask mask) {
        return RecipesHelper.mask(mask.sourceWidth, mask.indices);
    }

}
