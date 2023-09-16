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

import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.rete.aggregation.IndexerBasedAggregatorNode;
import tools.refinery.interpreter.rete.boundary.InputConnector;
import tools.refinery.interpreter.rete.eval.RelationEvaluatorNode;
import tools.refinery.interpreter.rete.index.DualInputNode;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.IterableIndexer;
import tools.refinery.interpreter.rete.index.ProjectionIndexer;
import tools.refinery.interpreter.rete.recipes.*;
import tools.refinery.interpreter.rete.remote.Address;
import tools.refinery.interpreter.rete.traceability.RecipeTraceInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class responsible for connecting freshly instantiating Rete nodes to their parents.
 *
 * @author Bergmann Gabor
 *
 */
class ConnectionFactory {
    ReteContainer reteContainer;

    public ConnectionFactory(ReteContainer reteContainer) {
        super();
        this.reteContainer = reteContainer;
    }

    // TODO move to node implementation instead?
    private boolean isStateful(ReteNodeRecipe recipe) {
        return recipe instanceof ProjectionIndexerRecipe || recipe instanceof IndexerBasedAggregatorRecipe
                || recipe instanceof SingleColumnAggregatorRecipe || recipe instanceof ExpressionEnforcerRecipe
                || recipe instanceof TransitiveClosureRecipe || recipe instanceof ProductionRecipe
                || recipe instanceof UniquenessEnforcerRecipe || recipe instanceof RelationEvaluationRecipe;

    }

    /**
     * PRE: nodes for parent recipes must already be created and registered
     * <p>
     * PRE: must not be an input node (for which {@link InputConnector} is responsible)
     */
    public void connectToParents(RecipeTraceInfo recipeTrace, Node freshNode) {
        final ReteNodeRecipe recipe = recipeTrace.getRecipe();
        if (recipe instanceof ConstantRecipe) {
            // NO-OP
        } else if (recipe instanceof InputRecipe) {
            throw new IllegalArgumentException(
                    ConnectionFactory.class.getSimpleName() + " not intended for input connection: " + recipe);
        } else if (recipe instanceof SingleParentNodeRecipe) {
            final Receiver receiver = (Receiver) freshNode;
            ReteNodeRecipe parentRecipe = ((SingleParentNodeRecipe) recipe).getParent();
            connectToParent(recipe, receiver, parentRecipe);
        } else if (recipe instanceof RelationEvaluationRecipe) {
            List<ReteNodeRecipe> parentRecipes = ((MultiParentNodeRecipe) recipe).getParents();
            List<Supplier> parentSuppliers = new ArrayList<Supplier>();
            for (final ReteNodeRecipe parentRecipe : parentRecipes) {
                parentSuppliers.add(getSupplierForRecipe(parentRecipe));
            }
            ((RelationEvaluatorNode) freshNode).connectToParents(parentSuppliers);
        } else if (recipe instanceof BetaRecipe) {
            final DualInputNode beta = (DualInputNode) freshNode;
            final ArrayList<RecipeTraceInfo> parentTraces = new ArrayList<RecipeTraceInfo>(
                    recipeTrace.getParentRecipeTraces());
            Slots slots = avoidActiveNodeConflict(parentTraces.get(0), parentTraces.get(1));
            beta.connectToIndexers(slots.primary, slots.secondary);
        } else if (recipe instanceof IndexerBasedAggregatorRecipe) {
            final IndexerBasedAggregatorNode aggregator = (IndexerBasedAggregatorNode) freshNode;
            final IndexerBasedAggregatorRecipe aggregatorRecipe = (IndexerBasedAggregatorRecipe) recipe;
            aggregator.initializeWith((ProjectionIndexer) resolveIndexer(aggregatorRecipe.getParent()));
        } else if (recipe instanceof MultiParentNodeRecipe) {
            final Receiver receiver = (Receiver) freshNode;
            List<ReteNodeRecipe> parentRecipes = ((MultiParentNodeRecipe) recipe).getParents();
            for (ReteNodeRecipe parentRecipe : parentRecipes) {
                connectToParent(recipe, receiver, parentRecipe);
            }
        }
    }

    private Indexer resolveIndexer(final IndexerRecipe indexerRecipe) {
        final Address<? extends Node> address = reteContainer.getNetwork().getExistingNodeByRecipe(indexerRecipe);
        return (Indexer) reteContainer.resolveLocal(address);
    }

    private void connectToParent(ReteNodeRecipe recipe, Receiver freshNode, ReteNodeRecipe parentRecipe) {
        final Supplier parentSupplier = getSupplierForRecipe(parentRecipe);

        // special synch
        if (freshNode instanceof ReinitializedNode) {
            Collection<Tuple> tuples = new ArrayList<Tuple>();
            parentSupplier.pullInto(tuples, true);
            ((ReinitializedNode) freshNode).reinitializeWith(tuples);
            reteContainer.connect(parentSupplier, freshNode);
        } else { // default case
            // stateless nodes do not have to be synced with contents UNLESS they already have children (recursive
            // corner case)
            if (isStateful(recipe)
                    || ((freshNode instanceof Supplier) && !((Supplier) freshNode).getReceivers().isEmpty())) {
                reteContainer.connectAndSynchronize(parentSupplier, freshNode);
            } else {
                // stateless node, no synch
                reteContainer.connect(parentSupplier, freshNode);
            }
        }
    }

    private Supplier getSupplierForRecipe(ReteNodeRecipe recipe) {
        @SuppressWarnings("unchecked")
        final Address<? extends Supplier> parentAddress = (Address<? extends Supplier>) reteContainer.getNetwork()
                .getExistingNodeByRecipe(recipe);
        final Supplier supplier = reteContainer.getProvisioner().asSupplier(parentAddress);
        return supplier;
    }

    /**
     * If two indexers share their active node, joining them via DualInputNode is error-prone. Exception: coincidence of
     * the two indexers is supported.
     *
     * @return a replacement for the secondary Indexers, if needed
     */
    private Slots avoidActiveNodeConflict(final RecipeTraceInfo primarySlot, final RecipeTraceInfo secondarySlot) {
        Slots result = new Slots();
        result.primary = (IterableIndexer) resolveIndexer((ProjectionIndexerRecipe) primarySlot.getRecipe());
        result.secondary = resolveIndexer((IndexerRecipe) secondarySlot.getRecipe());
        if (activeNodeConflict(result.primary, result.secondary)) {
			if (result.secondary instanceof IterableIndexer) {
				result.secondary = resolveActiveIndexer(secondarySlot);
			} else {
				result.primary = (IterableIndexer) resolveActiveIndexer(primarySlot);
			}
		}
        return result;
    }

    private Indexer resolveActiveIndexer(final RecipeTraceInfo inactiveIndexerTrace) {
        final RecipeTraceInfo activeIndexerTrace = reteContainer.getProvisioner()
                .accessActiveIndexer(inactiveIndexerTrace);
        reteContainer.getProvisioner().getOrCreateNodeByRecipe(activeIndexerTrace);
        return resolveIndexer((ProjectionIndexerRecipe) activeIndexerTrace.getRecipe());
    }

    private static class Slots {
        IterableIndexer primary;
        Indexer secondary;
    }

    /**
     * If two indexers share their active node, joining them via DualInputNode is error-prone. Exception: coincidence of
     * the two indexers is supported.
     *
     * @return true if there is a conflict of active nodes.
     */
    private boolean activeNodeConflict(Indexer primarySlot, Indexer secondarySlot) {
        return !primarySlot.equals(secondarySlot) && primarySlot.getActiveNode().equals(secondarySlot.getActiveNode());
    }

}
