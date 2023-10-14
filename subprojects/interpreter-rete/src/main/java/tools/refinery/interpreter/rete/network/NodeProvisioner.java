/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.network;

import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.boundary.InputConnector;
import tools.refinery.interpreter.rete.construction.plancompiler.CompilerHelper;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.OnetimeIndexer;
import tools.refinery.interpreter.rete.index.ProjectionIndexer;
import tools.refinery.interpreter.rete.network.delayed.DelayedConnectCommand;
import tools.refinery.interpreter.rete.recipes.*;
import tools.refinery.interpreter.rete.recipes.helper.RecipeRecognizer;
import tools.refinery.interpreter.rete.recipes.helper.RecipesHelper;
import tools.refinery.interpreter.rete.remote.Address;
import tools.refinery.interpreter.rete.remote.RemoteReceiver;
import tools.refinery.interpreter.rete.remote.RemoteSupplier;
import tools.refinery.interpreter.rete.traceability.ActiveNodeConflictTrace;
import tools.refinery.interpreter.rete.traceability.RecipeTraceInfo;
import tools.refinery.interpreter.rete.traceability.UserRequestTrace;
import tools.refinery.interpreter.rete.util.Options;

import java.util.Map;
import java.util.Set;

/**
 * Stores the internal parts of a rete network. Nodes are stored according to type and parameters.
 *
 * @author Gabor Bergmann
 */
public class NodeProvisioner {

    // boolean activeStorage = true;

    ReteContainer reteContainer;
    NodeFactory nodeFactory;
    ConnectionFactory connectionFactory;
    InputConnector inputConnector;
    IQueryRuntimeContext runtimeContext;

    // TODO as recipe?
    Map<Supplier, RemoteReceiver> remoteReceivers = CollectionsFactory.createMap();
    Map<Address<? extends Supplier>, RemoteSupplier> remoteSuppliers = CollectionsFactory.createMap();

    private RecipeRecognizer recognizer;

    /**
     * PRE: NodeFactory, ConnectionFactory must exist
     *
     * @param reteContainer
     *            the ReteNet whose interior is to be mapped.
     */
    public NodeProvisioner(ReteContainer reteContainer) {
        super();
        this.reteContainer = reteContainer;
        this.nodeFactory = reteContainer.getNodeFactory();
        this.connectionFactory = reteContainer.getConnectionFactory();
        this.inputConnector = reteContainer.getInputConnectionFactory();
        runtimeContext = reteContainer.getNetwork().getEngine().getRuntimeContext();
        recognizer = new RecipeRecognizer(runtimeContext);
    }

    public synchronized Address<? extends Node> getOrCreateNodeByRecipe(RecipeTraceInfo recipeTrace) {
        ReteNodeRecipe recipe = recipeTrace.getRecipe();
        Address<? extends Node> result = getNodesByRecipe().get(recipe);
        if (result == null) {
            // No node for this recipe object - but equivalent recipes still
            // reusable
            ReteNodeRecipe canonicalRecipe = recognizer.canonicalizeRecipe(recipe);
			result = getNodesByRecipe().get(canonicalRecipe);
			if (result == null) {
				if (canonicalRecipe.isConstructed()) {
					throw new IllegalStateException(
							"Already constructed node is missing for canonical recipe " + canonicalRecipe);
				}
				canonicalRecipe.setConstructed(true);
				final Node freshNode = instantiateNodeForRecipe(recipeTrace, canonicalRecipe);
				result = reteContainer.makeAddress(freshNode);
			}
			if (canonicalRecipe != recipe) {
				recipeTrace.shadowWithEquivalentRecipe(canonicalRecipe);
				getNodesByRecipe().put(recipe, result);
			}
        }
		if (getRecipeTraces().add(recipeTrace)) {
			result.getNodeCache().assignTraceInfo(recipeTrace);
		}
        return result;
    }

    private Set<RecipeTraceInfo> getRecipeTraces() {
        return reteContainer.network.recipeTraces;
    }

    private Node instantiateNodeForRecipe(RecipeTraceInfo recipeTrace, final ReteNodeRecipe recipe) {
        this.getRecipeTraces().add(recipeTrace);
        if (recipe instanceof IndexerRecipe) {

            // INSTANTIATE AND HOOK UP
            // (cannot delay hooking up, because parent determines indexer
            // implementation)
            ensureParents(recipeTrace);
            final ReteNodeRecipe parentRecipe = recipeTrace.getParentRecipeTraces().iterator().next().getRecipe();
            final Indexer result = nodeFactory.createIndexer(reteContainer, (IndexerRecipe) recipe,
                    asSupplier(
                            (Address<? extends Supplier>) reteContainer.network.getExistingNodeByRecipe(parentRecipe)),
                    recipeTrace);

            // REMEMBER
            if (Options.nodeSharingOption != Options.NodeSharingOption.NEVER) {
                getNodesByRecipe().put(recipe, reteContainer.makeAddress(result));
            }

            return result;
        } else {

            // INSTANTIATE
            Node result = nodeFactory.createNode(reteContainer, recipe, recipeTrace);

            // REMEMBER
            if (Options.nodeSharingOption == Options.NodeSharingOption.ALL) {
                getNodesByRecipe().put(recipe, reteContainer.makeAddress(result));
            }

            // HOOK UP
            // (recursion-tolerant due to this delayed order of initialization)
            if (recipe instanceof InputRecipe) {
                inputConnector.connectInput((InputRecipe) recipe, result);
            } else {
                if (recipe instanceof InputFilterRecipe)
                    inputConnector.connectInputFilter((InputFilterRecipe) recipe, result);
                ensureParents(recipeTrace);
                connectionFactory.connectToParents(recipeTrace, result);
            }
            return result;
        }
    }

    private Map<ReteNodeRecipe, Address<? extends Node>> getNodesByRecipe() {
        return reteContainer.network.nodesByRecipe;
    }

    private void ensureParents(RecipeTraceInfo recipeTrace) {
        for (RecipeTraceInfo parentTrace : recipeTrace.getParentRecipeTraces()) {
            getOrCreateNodeByRecipe(parentTrace);
        }
    }

    //// Remoting - TODO eliminate?

    synchronized RemoteReceiver accessRemoteReceiver(Address<? extends Supplier> address) {
        throw new UnsupportedOperationException("Multi-container Rete not supported yet");
        // if (!reteContainer.isLocal(address))
        // return
        // address.getContainer().getProvisioner().accessRemoteReceiver(address);
        // Supplier localSupplier = reteContainer.resolveLocal(address);
        // RemoteReceiver result = remoteReceivers.get(localSupplier);
        // if (result == null) {
        // result = new RemoteReceiver(reteContainer);
        // reteContainer.connect(localSupplier, result); // stateless node, no
        // // synch required
        //
        // if (Options.nodeSharingOption != Options.NodeSharingOption.NEVER)
        // remoteReceivers.put(localSupplier, result);
        // }
        // return result;
    }

    /**
     * @pre: address is NOT local
     */
    synchronized RemoteSupplier accessRemoteSupplier(Address<? extends Supplier> address) {
        throw new UnsupportedOperationException("Multi-container Rete not supported yet");
        // RemoteSupplier result = remoteSuppliers.get(address);
        // if (result == null) {
        // result = new RemoteSupplier(reteContainer,
        // address.getContainer().getProvisioner()
        // .accessRemoteReceiver(address));
        // // network.connectAndSynchronize(supplier, result);
        //
        // if (Options.nodeSharingOption != Options.NodeSharingOption.NEVER)
        // remoteSuppliers.put(address, result);
        // }
        // return result;
    }

    /**
     * The powerful method for accessing any (supplier) Address as a local supplier.
     */
    public Supplier asSupplier(Address<? extends Supplier> address) {
        if (!reteContainer.isLocal(address))
            return accessRemoteSupplier(address);
        else
            return reteContainer.resolveLocal(address);
    }

    /** the composite key tuple is formed as (RecipeTraceInfo, TupleMask) */
    private Map<Tuple, UserRequestTrace> projectionIndexerUserRequests = CollectionsFactory.createMap();

    // local version
    // TODO remove?
    public synchronized ProjectionIndexer accessProjectionIndexer(RecipeTraceInfo productionTrace, TupleMask mask) {
        Tuple tableKey = Tuples.staticArityFlatTupleOf(productionTrace, mask);
        UserRequestTrace indexerTrace = projectionIndexerUserRequests.computeIfAbsent(tableKey, k -> {
            final ProjectionIndexerRecipe projectionIndexerRecipe = projectionIndexerRecipe(
                    productionTrace, mask);
            return new UserRequestTrace(projectionIndexerRecipe, productionTrace);
        });
        final Address<? extends Node> address = getOrCreateNodeByRecipe(indexerTrace);
        return (ProjectionIndexer) reteContainer.resolveLocal(address);
    }

    // local version
    public synchronized ProjectionIndexer accessProjectionIndexerOnetime(RecipeTraceInfo supplierTrace,
            TupleMask mask) {
        if (Options.nodeSharingOption != Options.NodeSharingOption.NEVER)
            return accessProjectionIndexer(supplierTrace, mask);

        final Address<? extends Node> supplierAddress = getOrCreateNodeByRecipe(supplierTrace);
        Supplier supplier = (Supplier) reteContainer.resolveLocal(supplierAddress);

        OnetimeIndexer result = new OnetimeIndexer(reteContainer, mask);
        reteContainer.getDelayedCommandQueue().add(new DelayedConnectCommand(supplier, result, reteContainer));

        return result;
    }

    // local, read-only version
    public synchronized ProjectionIndexer peekProjectionIndexer(RecipeTraceInfo supplierTrace, TupleMask mask) {
        final Address<? extends Node> address = getNodesByRecipe().get(projectionIndexerRecipe(supplierTrace, mask));
        return address == null ? null : (ProjectionIndexer) reteContainer.resolveLocal(address);
    }

    private ProjectionIndexerRecipe projectionIndexerRecipe(
            RecipeTraceInfo parentTrace, TupleMask mask) {
        final ReteNodeRecipe parentRecipe = parentTrace.getRecipe();
        Tuple tableKey = Tuples.staticArityFlatTupleOf(parentRecipe, mask);
        ProjectionIndexerRecipe projectionIndexerRecipe = resultSeedRecipes.computeIfAbsent(tableKey, k ->
            RecipesHelper.projectionIndexerRecipe(parentRecipe, CompilerHelper.toRecipeMask(mask))
        );
        return projectionIndexerRecipe;
    }

    /** the composite key tuple is formed as (ReteNodeRecipe, TupleMask) */
    private Map<Tuple, ProjectionIndexerRecipe> resultSeedRecipes = CollectionsFactory.createMap();

    // public synchronized Address<? extends Supplier>
    // accessValueBinderFilterNode(
    // Address<? extends Supplier> supplierAddress, int bindingIndex, Object
    // bindingValue) {
    // Supplier supplier = asSupplier(supplierAddress);
    // Object[] paramsArray = { supplier.getNodeId(), bindingIndex, bindingValue
    // };
    // Tuple params = new FlatTuple(paramsArray);
    // ValueBinderFilterNode result = valueBinderFilters.get(params);
    // if (result == null) {
    // result = new ValueBinderFilterNode(reteContainer, bindingIndex,
    // bindingValue);
    // reteContainer.connect(supplier, result); // stateless node, no synch
    // // required
    //
    // if (Options.nodeSharingOption == Options.NodeSharingOption.ALL)
    // valueBinderFilters.put(params, result);
    // }
    // return reteContainer.makeAddress(result);
    // }

    /**
     * Returns a copy of the given indexer that is an active node by itself (created if does not exist). (Convention:
     * attached with same mask to a transparent node that is attached to parent node.) Node is created if it does not
     * exist yet.
     *
     * @return an identical but active indexer
     */
    // TODO rethink traceability
    RecipeTraceInfo accessActiveIndexer(RecipeTraceInfo inactiveIndexerRecipeTrace) {
        final RecipeTraceInfo parentRecipeTrace = inactiveIndexerRecipeTrace.getParentRecipeTraces().iterator().next();
        final ProjectionIndexerRecipe inactiveIndexerRecipe = (ProjectionIndexerRecipe) inactiveIndexerRecipeTrace
                .getRecipe();

        final TransparentRecipe transparentRecipe = RecipesFactory.eINSTANCE.createTransparentRecipe();
        transparentRecipe.setParent(parentRecipeTrace.getRecipe());
        final ActiveNodeConflictTrace transparentRecipeTrace = new ActiveNodeConflictTrace(transparentRecipe,
                parentRecipeTrace, inactiveIndexerRecipeTrace);

        final ProjectionIndexerRecipe activeIndexerRecipe = RecipesFactory.eINSTANCE
                .createProjectionIndexerRecipe();
        activeIndexerRecipe.setParent(transparentRecipe);
        activeIndexerRecipe.setMask(inactiveIndexerRecipe.getMask());
        final ActiveNodeConflictTrace activeIndexerRecipeTrace = new ActiveNodeConflictTrace(activeIndexerRecipe,
                transparentRecipeTrace, inactiveIndexerRecipeTrace);

        return activeIndexerRecipeTrace;
    }

    // /**
    // * @param parent
    // * @return
    // */
    // private TransparentNode accessTransparentNodeInternal(Supplier parent) {
    // nodeFactory.
    // return null;
    // }

    // public synchronized void registerSpecializedProjectionIndexer(Node node,
    // ProjectionIndexer indexer) {
    // if (Options.nodeSharingOption != Options.NodeSharingOption.NEVER) {
    // Object[] paramsArray = { node.getNodeId(), indexer.getMask() };
    // Tuple params = new FlatTuple(paramsArray);
    // projectionIndexers.put(params, indexer);
    // }
    // }

}
