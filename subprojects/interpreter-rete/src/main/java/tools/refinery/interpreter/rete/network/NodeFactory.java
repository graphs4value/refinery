/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EMap;
import tools.refinery.interpreter.matchers.context.IPosetComparator;
import tools.refinery.interpreter.matchers.psystem.IExpressionEvaluator;
import tools.refinery.interpreter.matchers.psystem.IRelationEvaluator;
import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.rete.aggregation.ColumnAggregatorNode;
import tools.refinery.interpreter.rete.aggregation.CountNode;
import tools.refinery.interpreter.rete.aggregation.IAggregatorNode;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulParallelTimelyColumnAggregatorNode;
import tools.refinery.interpreter.rete.aggregation.timely.FaithfulSequentialTimelyColumnAggregatorNode;
import tools.refinery.interpreter.rete.aggregation.timely.FirstOnlyParallelTimelyColumnAggregatorNode;
import tools.refinery.interpreter.rete.aggregation.timely.FirstOnlySequentialTimelyColumnAggregatorNode;
import tools.refinery.interpreter.rete.boundary.ExternalInputEnumeratorNode;
import tools.refinery.interpreter.rete.boundary.ExternalInputStatelessFilterNode;
import tools.refinery.interpreter.rete.eval.EvaluatorCore;
import tools.refinery.interpreter.rete.eval.MemorylessEvaluatorNode;
import tools.refinery.interpreter.rete.eval.OutputCachingEvaluatorNode;
import tools.refinery.interpreter.rete.eval.RelationEvaluatorNode;
import tools.refinery.interpreter.rete.index.ExistenceNode;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.JoinNode;
import tools.refinery.interpreter.rete.itc.alg.representative.RepresentativeElectionAlgorithm;
import tools.refinery.interpreter.rete.itc.alg.representative.StronglyConnectedComponentAlgorithm;
import tools.refinery.interpreter.rete.itc.alg.representative.WeaklyConnectedComponentAlgorithm;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration.AggregatorArchitecture;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration.TimelineRepresentation;
import tools.refinery.interpreter.rete.misc.ConstantNode;
import tools.refinery.interpreter.rete.recipes.*;
import tools.refinery.interpreter.rete.single.*;
import tools.refinery.interpreter.rete.traceability.TraceInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for instantiating Rete nodes. The created nodes are not connected to the network yet.
 *
 * @author Bergmann Gabor
 *
 */
class NodeFactory {
    Logger logger;

    public NodeFactory(Logger logger) {
        super();
        this.logger = logger;
    }

    /**
     * PRE: parent node must already be created
     */
    public Indexer createIndexer(ReteContainer reteContainer, IndexerRecipe recipe, Supplier parentNode,
                                 TraceInfo... traces) {

        if (recipe instanceof ProjectionIndexerRecipe) {
            return parentNode.constructIndex(toMask(recipe.getMask()), traces);
            // already traced
        } else if (recipe instanceof AggregatorIndexerRecipe) {
            int indexOfAggregateResult = recipe.getParent().getArity();
            int resultPosition = recipe.getMask().getSourceIndices().lastIndexOf(indexOfAggregateResult);

            IAggregatorNode aggregatorNode = (IAggregatorNode) parentNode;
            final Indexer result = (resultPosition == -1) ? aggregatorNode.getAggregatorOuterIndexer()
                    : aggregatorNode.getAggregatorOuterIdentityIndexer(resultPosition);

            for (TraceInfo traceInfo : traces)
                result.assignTraceInfo(traceInfo);
            return result;
        } else
            throw new IllegalArgumentException("Unkown Indexer recipe: " + recipe);
    }

    /**
     * PRE: recipe is not an indexer recipe.
     */
    public Supplier createNode(ReteContainer reteContainer, ReteNodeRecipe recipe, TraceInfo... traces) {
        if (recipe instanceof IndexerRecipe)
            throw new IllegalArgumentException("Indexers are not created by NodeFactory: " + recipe);

        Supplier result = instantiateNodeDispatch(reteContainer, recipe);
        for (TraceInfo traceInfo : traces)
            result.assignTraceInfo(traceInfo);
        return result;
    }

    private Supplier instantiateNodeDispatch(ReteContainer reteContainer, ReteNodeRecipe recipe) {

        // Parentless

        if (recipe instanceof ConstantRecipe)
            return instantiateNode(reteContainer, (ConstantRecipe) recipe);
        if (recipe instanceof InputRecipe)
            return instantiateNode(reteContainer, (InputRecipe) recipe);

        // SingleParentNodeRecipe

        // if (recipe instanceof ProjectionIndexer)
        // return instantiateNode((ProjectionIndexer)recipe);
        if (recipe instanceof InputFilterRecipe)
            return instantiateNode(reteContainer, (InputFilterRecipe) recipe);
        if (recipe instanceof InequalityFilterRecipe)
            return instantiateNode(reteContainer, (InequalityFilterRecipe) recipe);
        if (recipe instanceof EqualityFilterRecipe)
            return instantiateNode(reteContainer, (EqualityFilterRecipe) recipe);
        if (recipe instanceof TransparentRecipe)
            return instantiateNode(reteContainer, (TransparentRecipe) recipe);
        if (recipe instanceof TrimmerRecipe)
            return instantiateNode(reteContainer, (TrimmerRecipe) recipe);
        if (recipe instanceof TransitiveClosureRecipe)
            return instantiateNode(reteContainer, (TransitiveClosureRecipe) recipe);
		if (recipe instanceof RepresentativeElectionRecipe)
			return instantiateNode(reteContainer, (RepresentativeElectionRecipe) recipe);
        if (recipe instanceof RelationEvaluationRecipe)
            return instantiateNode(reteContainer, (RelationEvaluationRecipe) recipe);
        if (recipe instanceof ExpressionEnforcerRecipe)
            return instantiateNode(reteContainer, (ExpressionEnforcerRecipe) recipe);
        if (recipe instanceof CountAggregatorRecipe)
            return instantiateNode(reteContainer, (CountAggregatorRecipe) recipe);
        if (recipe instanceof SingleColumnAggregatorRecipe)
            return instantiateNode(reteContainer, (SingleColumnAggregatorRecipe) recipe);
        if (recipe instanceof DiscriminatorDispatcherRecipe)
            return instantiateNode(reteContainer, (DiscriminatorDispatcherRecipe) recipe);
        if (recipe instanceof DiscriminatorBucketRecipe)
            return instantiateNode(reteContainer, (DiscriminatorBucketRecipe) recipe);

        // MultiParentNodeRecipe
        if (recipe instanceof UniquenessEnforcerRecipe)
            return instantiateNode(reteContainer, (UniquenessEnforcerRecipe) recipe);
        if (recipe instanceof ProductionRecipe)
            return instantiateNode(reteContainer, (ProductionRecipe) recipe);

        // BetaNodeRecipe
        if (recipe instanceof JoinRecipe)
            return instantiateNode(reteContainer, (JoinRecipe) recipe);
        if (recipe instanceof SemiJoinRecipe)
            return instantiateNode(reteContainer, (SemiJoinRecipe) recipe);
        if (recipe instanceof AntiJoinRecipe)
            return instantiateNode(reteContainer, (AntiJoinRecipe) recipe);

        // ... else
        throw new IllegalArgumentException("Unsupported recipe type: " + recipe);
    }

    // INSTANTIATION for recipe types

    private Supplier instantiateNode(ReteContainer reteContainer, InputRecipe recipe) {
        return new ExternalInputEnumeratorNode(reteContainer);
    }

    private Supplier instantiateNode(ReteContainer reteContainer, InputFilterRecipe recipe) {
        return new ExternalInputStatelessFilterNode(reteContainer, toMaskOrNull(recipe.getMask()));
    }

    private Supplier instantiateNode(ReteContainer reteContainer, CountAggregatorRecipe recipe) {
        return new CountNode(reteContainer);
    }

    private Supplier instantiateNode(ReteContainer reteContainer, TransparentRecipe recipe) {
        return new TransparentNode(reteContainer);
    }

    private Supplier instantiateNode(ReteContainer reteContainer, ExpressionEnforcerRecipe recipe) {
        final IExpressionEvaluator evaluator = toIExpressionEvaluator(recipe.getExpression());
        final Map<String, Integer> posMapping = toStringIndexMap(recipe.getMappedIndices());
        final int sourceTupleWidth = recipe.getParent().getArity();
        EvaluatorCore core = null;
        if (recipe instanceof CheckRecipe) {
            core = new EvaluatorCore.PredicateEvaluatorCore(logger, evaluator, posMapping, sourceTupleWidth);
        } else if (recipe instanceof EvalRecipe) {
            final boolean isUnwinding = ((EvalRecipe) recipe).isUnwinding();
            core = new EvaluatorCore.FunctionEvaluatorCore(logger, evaluator, posMapping, sourceTupleWidth, isUnwinding);
        } else {
            throw new IllegalArgumentException("Unhandled expression enforcer recipe: " + recipe.getClass() + "!");
        }
        if (recipe.isCacheOutput()) {
            return new OutputCachingEvaluatorNode(reteContainer, core);
        } else {
            return new MemorylessEvaluatorNode(reteContainer, core);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Supplier instantiateNode(ReteContainer reteContainer, SingleColumnAggregatorRecipe recipe) {
        final IMultisetAggregationOperator operator = recipe.getMultisetAggregationOperator();
        TupleMask coreMask = null;
        if (recipe.getOptionalMonotonicityInfo() != null) {
            coreMask = toMask(recipe.getOptionalMonotonicityInfo().getCoreMask());
        } else {
            coreMask = toMask(recipe.getGroupByMask());
        }

        if (reteContainer.isTimelyEvaluation()) {
            final TimelyConfiguration timelyConfiguration = reteContainer.getTimelyConfiguration();
            final AggregatorArchitecture aggregatorArchitecture = timelyConfiguration.getAggregatorArchitecture();
            final TimelineRepresentation timelineRepresentation = timelyConfiguration.getTimelineRepresentation();

            TupleMask posetMask = null;

            if (recipe.getOptionalMonotonicityInfo() != null) {
                posetMask = toMask(recipe.getOptionalMonotonicityInfo().getPosetMask());
            } else {
                final int aggregatedColumn = recipe.getAggregableIndex();
                posetMask = TupleMask.selectSingle(aggregatedColumn, coreMask.sourceWidth);
            }

            if (timelineRepresentation == TimelineRepresentation.FIRST_ONLY
                    && aggregatorArchitecture == AggregatorArchitecture.SEQUENTIAL) {
                return new FirstOnlySequentialTimelyColumnAggregatorNode(reteContainer, operator, coreMask, posetMask);
            } else if (timelineRepresentation == TimelineRepresentation.FIRST_ONLY
                    && aggregatorArchitecture == AggregatorArchitecture.PARALLEL) {
                return new FirstOnlyParallelTimelyColumnAggregatorNode(reteContainer, operator, coreMask, posetMask);
            } else if (timelineRepresentation == TimelineRepresentation.FAITHFUL
                    && aggregatorArchitecture == AggregatorArchitecture.SEQUENTIAL) {
                return new FaithfulSequentialTimelyColumnAggregatorNode(reteContainer, operator, coreMask, posetMask);
            } else if (timelineRepresentation == TimelineRepresentation.FAITHFUL
                    && aggregatorArchitecture == AggregatorArchitecture.PARALLEL) {
                return new FaithfulParallelTimelyColumnAggregatorNode(reteContainer, operator, coreMask, posetMask);
            } else {
                throw new IllegalArgumentException("Unsupported timely configuration!");
            }
        } else if (recipe.isDeleteRederiveEvaluation() && recipe.getOptionalMonotonicityInfo() != null) {
            final TupleMask posetMask = toMask(recipe.getOptionalMonotonicityInfo().getPosetMask());
            final IPosetComparator posetComparator = (IPosetComparator) recipe.getOptionalMonotonicityInfo()
                    .getPosetComparator();
            return new ColumnAggregatorNode(reteContainer, operator, recipe.isDeleteRederiveEvaluation(), coreMask,
                    posetMask, posetComparator);
        } else {
            final int aggregatedColumn = recipe.getAggregableIndex();
            return new ColumnAggregatorNode(reteContainer, operator, coreMask, aggregatedColumn);
        }
    }

    private Supplier instantiateNode(ReteContainer reteContainer, TransitiveClosureRecipe recipe) {
        return new TransitiveClosureNode(reteContainer);
    }

	private Supplier instantiateNode(ReteContainer reteContainer, RepresentativeElectionRecipe recipe) {
		RepresentativeElectionAlgorithm.Factory algorithmFactory = switch (recipe.getConnectivity()) {
			case STRONG -> StronglyConnectedComponentAlgorithm::new;
			case WEAK -> WeaklyConnectedComponentAlgorithm::new;
		};
		return new RepresentativeElectionNode(reteContainer, algorithmFactory);
	}

    private Supplier instantiateNode(ReteContainer reteContainer, RelationEvaluationRecipe recipe) {
        return new RelationEvaluatorNode(reteContainer, toIRelationEvaluator(recipe.getEvaluator()));
    }

    private Supplier instantiateNode(ReteContainer reteContainer, ProductionRecipe recipe) {
        if (reteContainer.isTimelyEvaluation()) {
            return new TimelyProductionNode(reteContainer, toStringIndexMap(recipe.getMappedIndices()));
        } else if (recipe.isDeleteRederiveEvaluation() && recipe.getOptionalMonotonicityInfo() != null) {
            TupleMask coreMask = toMask(recipe.getOptionalMonotonicityInfo().getCoreMask());
            TupleMask posetMask = toMask(recipe.getOptionalMonotonicityInfo().getPosetMask());
            IPosetComparator posetComparator = (IPosetComparator) recipe.getOptionalMonotonicityInfo()
                    .getPosetComparator();
            return new DefaultProductionNode(reteContainer, toStringIndexMap(recipe.getMappedIndices()),
                    recipe.isDeleteRederiveEvaluation(), coreMask, posetMask, posetComparator);
        } else {
            return new DefaultProductionNode(reteContainer, toStringIndexMap(recipe.getMappedIndices()),
                    recipe.isDeleteRederiveEvaluation());
        }
    }

    private Supplier instantiateNode(ReteContainer reteContainer, UniquenessEnforcerRecipe recipe) {
        if (reteContainer.isTimelyEvaluation()) {
            return new TimelyUniquenessEnforcerNode(reteContainer, recipe.getArity());
        } else if (recipe.isDeleteRederiveEvaluation() && recipe.getOptionalMonotonicityInfo() != null) {
            TupleMask coreMask = toMask(recipe.getOptionalMonotonicityInfo().getCoreMask());
            TupleMask posetMask = toMask(recipe.getOptionalMonotonicityInfo().getPosetMask());
            IPosetComparator posetComparator = (IPosetComparator) recipe.getOptionalMonotonicityInfo()
                    .getPosetComparator();
            return new UniquenessEnforcerNode(reteContainer, recipe.getArity(), recipe.isDeleteRederiveEvaluation(),
                    coreMask, posetMask, posetComparator);
        } else {
            return new UniquenessEnforcerNode(reteContainer, recipe.getArity(), recipe.isDeleteRederiveEvaluation());
        }
    }

    private Supplier instantiateNode(ReteContainer reteContainer, ConstantRecipe recipe) {
        final List<Object> constantValues = recipe.getConstantValues();
        final Object[] constantArray = constantValues.toArray(new Object[constantValues.size()]);
        return new ConstantNode(reteContainer, Tuples.flatTupleOf(constantArray));
    }

    private Supplier instantiateNode(ReteContainer reteContainer, DiscriminatorBucketRecipe recipe) {
        return new DiscriminatorBucketNode(reteContainer, recipe.getBucketKey());
    }

    private Supplier instantiateNode(ReteContainer reteContainer, DiscriminatorDispatcherRecipe recipe) {
        return new DiscriminatorDispatcherNode(reteContainer, recipe.getDiscriminationColumnIndex());
    }

    private Supplier instantiateNode(ReteContainer reteContainer, TrimmerRecipe recipe) {
        return new TrimmerNode(reteContainer, toMask(recipe.getMask()));
    }

    private Supplier instantiateNode(ReteContainer reteContainer, InequalityFilterRecipe recipe) {
        Tunnel result = new InequalityFilterNode(reteContainer, recipe.getSubject(),
                TupleMask.fromSelectedIndices(recipe.getParent().getArity(), recipe.getInequals()));
        return result;
    }

    private Supplier instantiateNode(ReteContainer reteContainer, EqualityFilterRecipe recipe) {
        final int[] equalIndices = TupleMask.integersToIntArray(recipe.getIndices());
        return new EqualityFilterNode(reteContainer, equalIndices);
    }

    private Supplier instantiateNode(ReteContainer reteContainer, AntiJoinRecipe recipe) {
        return new ExistenceNode(reteContainer, true);
    }

    private Supplier instantiateNode(ReteContainer reteContainer, SemiJoinRecipe recipe) {
        return new ExistenceNode(reteContainer, false);
    }

    private Supplier instantiateNode(ReteContainer reteContainer, JoinRecipe recipe) {
        return new JoinNode(reteContainer, toMask(recipe.getRightParentComplementaryMask()));
    }

    // HELPERS

    private IExpressionEvaluator toIExpressionEvaluator(ExpressionDefinition expressionDefinition) {
        final Object evaluator = expressionDefinition.getEvaluator();
        if (evaluator instanceof IExpressionEvaluator) {
            return (IExpressionEvaluator) evaluator;
        }
        throw new IllegalArgumentException("No runtime support for expression evaluator: " + evaluator);
    }

    private IRelationEvaluator toIRelationEvaluator(ExpressionDefinition expressionDefinition) {
        final Object evaluator = expressionDefinition.getEvaluator();
        if (evaluator instanceof IRelationEvaluator) {
            return (IRelationEvaluator) evaluator;
        }
        throw new IllegalArgumentException("No runtime support for relation evaluator: " + evaluator);
    }

    private Map<String, Integer> toStringIndexMap(final EMap<String, Integer> mappedIndices) {
        final HashMap<String, Integer> result = new HashMap<String, Integer>();
        for (java.util.Map.Entry<String, Integer> entry : mappedIndices) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** Mask can be null */
    private TupleMask toMaskOrNull(Mask mask) {
        if (mask == null)
            return null;
        else
            return toMask(mask);
    }

    /** Mask is non-null. */
    private TupleMask toMask(Mask mask) {
        return TupleMask.fromSelectedIndices(mask.getSourceArity(), mask.getSourceIndices());
    }

}
