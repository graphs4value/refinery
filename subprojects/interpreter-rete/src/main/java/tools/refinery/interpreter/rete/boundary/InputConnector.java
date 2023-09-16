/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.boundary;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.network.Network;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.recipes.InputFilterRecipe;
import tools.refinery.interpreter.rete.recipes.InputRecipe;
import tools.refinery.interpreter.rete.remote.Address;

/**
 * A class responsible for connecting input nodes to the runtime context.
 *
 * @author Bergmann Gabor
 *
 */
public final class InputConnector {
    Network network;

    private Map<IInputKey, Map<Tuple, Address<ExternalInputEnumeratorNode>>> externalInputRoots = CollectionsFactory.createMap();

//    /*
//     * arity:1 used as simple entity constraints label is the object representing the type null label means all entities
//     * regardless of type (global supertype), if allowed
//     */
//    protected Map<Object, Address<? extends Tunnel>> unaryRoots = CollectionsFactory.getMap();
//    /*
//     * arity:3 (rel, from, to) used as VPM relation constraints null label means all relations regardless of type
//     * (global supertype)
//     */
//    protected Map<Object, Address<? extends Tunnel>> ternaryEdgeRoots = CollectionsFactory.getMap();
//    /*
//     * arity:2 (from, to) not used over VPM; can be used as EMF references for instance label is the object representing
//     * the type null label means all entities regardless of type if allowed (global supertype), if allowed
//     */
//    protected Map<Object, Address<? extends Tunnel>> binaryEdgeRoots = CollectionsFactory.getMap();
//
//    protected Address<? extends Tunnel> containmentRoot = null;
//    protected Address<? extends Supplier> containmentTransitiveRoot = null;
//    protected Address<? extends Tunnel> instantiationRoot = null;
//    protected Address<? extends Supplier> instantiationTransitiveRoot = null;
//    protected Address<? extends Tunnel> generalizationRoot = null;
//    protected Address<? extends Supplier> generalizationTransitiveRoot = null;


    public InputConnector(Network network) {
        super();
        this.network = network;
    }


    public Network getNetwork() {
        return network;
    }


    /**
     * Connects a given input filter node to the external input source.
     */
    public void connectInputFilter(InputFilterRecipe recipe, Node freshNode) {
        final ExternalInputStatelessFilterNode inputNode = (ExternalInputStatelessFilterNode)freshNode;

        IInputKey inputKey = (IInputKey) recipe.getInputKey();
        inputNode.connectThroughContext(network.getEngine(), inputKey);
    }


    /**
     * Connects a given input enumerator node to the external input source.
     */
    public void connectInput(InputRecipe recipe, Node freshNode) {
        final ExternalInputEnumeratorNode inputNode = (ExternalInputEnumeratorNode)freshNode;

        IInputKey inputKey = (IInputKey) recipe.getInputKey();
        Tuple seed = nopSeed(inputKey); // no preseeding as of now
        final Address<ExternalInputEnumeratorNode> freshAddress = Address.of(inputNode);
        externalInputRoots.computeIfAbsent(inputKey, k -> CollectionsFactory.createMap()).put(seed, freshAddress);
        inputNode.connectThroughContext(network.getEngine(), inputKey, seed);

//		final Address<Tunnel> freshAddress = Address.of((Tunnel)freshNode);
//		if (recipe instanceof TypeInputRecipe) {
//			final Object typeKey = ((TypeInputRecipe) recipe).getTypeKey();
//
//			if (recipe instanceof UnaryInputRecipe) {
//				unaryRoots.put(typeKey, freshAddress);
//				new EntityFeeder(freshAddress, this, typeKey).feed();
////		        if (typeObject != null && generalizationQueryDirection == GeneralizationQueryDirection.BOTH) {
////		            Collection<? extends Object> subTypes = context.enumerateDirectUnarySubtypes(typeObject);
////
////		            for (Object subType : subTypes) {
////		                Address<? extends Tunnel> subRoot = accessUnaryRoot(subType);
////		                network.connectRemoteNodes(subRoot, tn, true);
////		            }
////		        }
//			} else if (recipe instanceof BinaryInputRecipe) {
//				binaryEdgeRoots.put(typeKey, freshAddress);
//				externalInputRoots.put(rowKey, columnKey, freshAddress);
//				new ReferenceFeeder(freshAddress, this, typeKey).feed();
//				//        if (typeObject != null && generalizationQueryDirection == GeneralizationQueryDirection.BOTH) {
//				//            Collection<? extends Object> subTypes = context.enumerateDirectTernaryEdgeSubtypes(typeObject);
//				//
//				//            for (Object subType : subTypes) {
//				//                Address<? extends Tunnel> subRoot = accessTernaryEdgeRoot(subType);
//				//                network.connectRemoteNodes(subRoot, tn, true);
//				//            }
//				//        }
//			}
//
//
//		}

    }

//    /**
//     * fetches the entity Root node under specified label; returns null if it doesn't exist yet
//     */
//    public Address<? extends Tunnel> getUnaryRoot(Object label) {
//        return unaryRoots.get(label);
//    }
//
//    public Collection<Address<? extends Tunnel>> getAllUnaryRoots() {
//        return unaryRoots.values();
//    }
//
//    /**
//     * fetches the relation Root node under specified label; returns null if it doesn't exist yet
//     */
//    public Address<? extends Tunnel> getTernaryEdgeRoot(Object label) {
//        return ternaryEdgeRoots.get(label);
//    }
//
//    public Collection<Address<? extends Tunnel>> getAllTernaryEdgeRoots() {
//        return ternaryEdgeRoots.values();
//    }
//
//    /**
//     * fetches the reference Root node under specified label; returns null if it doesn't exist yet
//     */
//    public Address<? extends Tunnel> getBinaryEdgeRoot(Object label) {
//        return binaryEdgeRoots.get(label);
//    }
//
//    public Collection<Address<? extends Tunnel>> getAllBinaryEdgeRoots() {
//        return binaryEdgeRoots.values();
//    }
//
//
//	public Address<? extends Tunnel> getContainmentRoot() {
//		return containmentRoot;
//	}
//
//
//	public Address<? extends Supplier> getContainmentTransitiveRoot() {
//		return containmentTransitiveRoot;
//	}
//
//
//	public Address<? extends Tunnel> getInstantiationRoot() {
//		return instantiationRoot;
//	}
//
//
//	public Address<? extends Supplier> getInstantiationTransitiveRoot() {
//		return instantiationTransitiveRoot;
//	}
//
//
//	public Address<? extends Tunnel> getGeneralizationRoot() {
//		return generalizationRoot;
//	}


    public Stream<Address<ExternalInputEnumeratorNode>> getAllExternalInputNodes() {
        return externalInputRoots.values().stream().flatMap(map -> map.values().stream());
    }
    public Collection<Address<ExternalInputEnumeratorNode>> getAllExternalInputNodesForKey(IInputKey inputKey) {
        return externalInputRoots.getOrDefault(inputKey, Collections.emptyMap()).values();
    }
    public Address<ExternalInputEnumeratorNode> getExternalInputNodeForKeyUnseeded(IInputKey inputKey) {
        return externalInputRoots.getOrDefault(inputKey, Collections.emptyMap()).get(nopSeed(inputKey));
    }
    public Address<ExternalInputEnumeratorNode> getExternalInputNode(IInputKey inputKey, Tuple seed) {
        if (seed == null) seed = nopSeed(inputKey);
        return externalInputRoots.getOrDefault(inputKey, Collections.emptyMap()).get(seed);
    }


    Tuple nopSeed(IInputKey inputKey) {
        return Tuples.flatTupleOf(new Object[inputKey.getArity()]);
    }


}
