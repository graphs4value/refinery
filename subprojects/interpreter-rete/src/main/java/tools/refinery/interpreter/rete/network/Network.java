/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.network;

import tools.refinery.interpreter.rete.boundary.InputConnector;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.rete.matcher.ReteEngine;
import tools.refinery.interpreter.rete.remote.Address;
import tools.refinery.interpreter.rete.traceability.RecipeTraceInfo;
import tools.refinery.interpreter.rete.util.Options;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Gabor Bergmann
 *
 */
public class Network {
    final int threads;

    protected ArrayList<ReteContainer> containers;
    ReteContainer headContainer;
    private int firstContainer = 0;
    private int nextContainer = 0;

    // the following fields exist only if threads > 0
    protected final Map<ReteContainer, Long> globalTerminationCriteria;
    protected Map<ReteContainer, Long> reportedClocks = null;
    protected Lock updateLock = null; // grab during normal update operations
    protected Lock structuralChangeLock = null; // grab if the network structure
                                                // is to
    // be changed

    // Knowledge of the outside world
    private ReteEngine engine;
    protected NodeFactory nodeFactory;
    protected InputConnector inputConnector;

    // Node and recipe administration
    // incl. addresses for existing nodes by recipe (where available)
    // Maintained by NodeProvisioner of each container
    Map<ReteNodeRecipe, Address<? extends Node>> nodesByRecipe = CollectionsFactory.createMap();
    Set<RecipeTraceInfo> recipeTraces = CollectionsFactory.createSet();

    /**
     * @throws IllegalStateException
     *             if no node has been constructed for the recipe
     */
    public synchronized Address<? extends Node> getExistingNodeByRecipe(ReteNodeRecipe recipe) {
        final Address<? extends Node> node = nodesByRecipe.get(recipe);
        if (node == null)
            throw new IllegalStateException(String.format("Rete node for recipe %s not constructed yet.", recipe));
        return node;
    }

    /**
     * @return null if no node has been constructed for the recipe
     */
    public synchronized Address<? extends Node> getNodeByRecipeIfExists(ReteNodeRecipe recipe) {
        final Address<? extends Node> node = nodesByRecipe.get(recipe);
        return node;
    }

    /**
     * @param threads
     *            the number of threads to operate the network with; 0 means single-threaded operation, 1 starts an
     *            asynchronous thread to operate the RETE net, >1 uses multiple RETE containers.
     */
    public Network(int threads, ReteEngine engine) {
        super();
        this.threads = threads;
        this.engine = engine;
        this.inputConnector = new InputConnector(this);
        this.nodeFactory = new NodeFactory(engine.getLogger());

        containers = new ArrayList<ReteContainer>();
        firstContainer = (threads > 1) ? Options.firstFreeContainer : 0; // NOPMD
        nextContainer = firstContainer;

        if (threads > 0) {
            globalTerminationCriteria = CollectionsFactory.createMap();
            reportedClocks = CollectionsFactory.createMap();
            ReadWriteLock rwl = new ReentrantReadWriteLock();
            updateLock = rwl.readLock();
            structuralChangeLock = rwl.writeLock();
            for (int i = 0; i < threads; ++i)
                containers.add(new ReteContainer(this, true));
        } else {
			containers.add(new ReteContainer(this, false));
			globalTerminationCriteria = null;
		}
        headContainer = containers.get(0);
    }

    /**
     * Kills this Network along with all containers and message consumption cycles.
     */
    public void kill() {
        for (ReteContainer container : containers) {
            container.kill();
        }
        containers.clear();
    }

    /**
     * Returns the head container, that is guaranteed to reside in the same JVM as the Network object.
     */
    public ReteContainer getHeadContainer() {
        return headContainer;
    }

    /**
     * Returns the next container in round-robin fashion. Configurable not to yield head container.
     */
    public ReteContainer getNextContainer() {
        if (nextContainer >= containers.size())
            nextContainer = firstContainer;
        return containers.get(nextContainer++);
    }

    /**
     * Internal message delivery method.
     *
     * @pre threads > 0
     */
    private void sendUpdate(Address<? extends Receiver> receiver, Direction direction, Tuple updateElement) {
        ReteContainer affectedContainer = receiver.getContainer();
        synchronized (globalTerminationCriteria) {
            long newCriterion = affectedContainer.sendUpdateToLocalAddress(receiver, direction, updateElement);
            terminationCriterion(affectedContainer, newCriterion);
        }
    }

    /**
     * Internal message delivery method for single-threaded operation
     *
     * @pre threads == 0
     */
    private void sendUpdateSingleThreaded(Address<? extends Receiver> receiver, Direction direction,
            Tuple updateElement) {
        ReteContainer affectedContainer = receiver.getContainer();
        affectedContainer.sendUpdateToLocalAddressSingleThreaded(receiver, direction, updateElement);
    }

    /**
     * Internal message delivery method.
     *
     * @pre threads > 0
     */
    private void sendUpdates(Address<? extends Receiver> receiver, Direction direction,
            Collection<Tuple> updateElements) {
        if (updateElements.isEmpty())
            return;
        ReteContainer affectedContainer = receiver.getContainer();
        synchronized (globalTerminationCriteria) {
            long newCriterion = affectedContainer.sendUpdatesToLocalAddress(receiver, direction, updateElements);
            terminationCriterion(affectedContainer, newCriterion);
        }
    }

    /**
     * Sends an update message to the receiver node, indicating a newly found or lost partial matching. The node may
     * reside in any of the containers associated with this network. To be called from a user thread during normal
     * operation, NOT during construction.
     *
     * @since 2.4
     */
    public void sendExternalUpdate(Address<? extends Receiver> receiver, Direction direction, Tuple updateElement) {
        if (threads > 0) {
            try {
                updateLock.lock();
                sendUpdate(receiver, direction, updateElement);
            } finally {
                updateLock.unlock();
            }
        } else {
            sendUpdateSingleThreaded(receiver, direction, updateElement);
            // getHeadContainer().
        }
    }

    /**
     * Sends an update message to the receiver node, indicating a newly found or lost partial matching. The node may
     * reside in any of the containers associated with this network. To be called from a user thread during
     * construction.
     *
     * @pre: structuralChangeLock MUST be grabbed by the sequence (but not necessarily this thread, as the sequence may
     *       span through network calls, that's why it's not enforced here )
     *
     * @return the value of the target container's clock at the time when the message was accepted into its message
     *         queue
     * @since 2.4
     */
    public void sendConstructionUpdate(Address<? extends Receiver> receiver, Direction direction, Tuple updateElement) {
        // structuralChangeLock.lock();
        if (threads > 0)
            sendUpdate(receiver, direction, updateElement);
        else
            receiver.getContainer().sendUpdateToLocalAddressSingleThreaded(receiver, direction, updateElement);
        // structuralChangeLock.unlock();
    }

    /**
     * Sends multiple update messages atomically to the receiver node, indicating a newly found or lost partial
     * matching. The node may reside in any of the containers associated with this network. To be called from a user
     * thread during construction.
     *
     * @pre: structuralChangeLock MUST be grabbed by the sequence (but not necessarily this thread, as the sequence may
     *       span through network calls, that's why it's not enforced here )
     *
     * @since 2.4
     */
    public void sendConstructionUpdates(Address<? extends Receiver> receiver, Direction direction,
            Collection<Tuple> updateElements) {
        // structuralChangeLock.lock();
        if (threads > 0)
            sendUpdates(receiver, direction, updateElements);
        else
            receiver.getContainer().sendUpdatesToLocalAddressSingleThreaded(receiver, direction, updateElements);
        // structuralChangeLock.unlock();
    }

    /**
     * Establishes connection between a supplier and a receiver node, regardless which container they are in. Not to be
     * called remotely, because this method enforces the structural lock.
     *
     * @param supplier
     * @param receiver
     * @param synchronise
     *            indicates whether the receiver should be synchronised to the current contents of the supplier
     */
    public void connectRemoteNodes(Address<? extends Supplier> supplier, Address<? extends Receiver> receiver,
            boolean synchronise) {
        try {
            if (threads > 0)
                structuralChangeLock.lock();
            receiver.getContainer().connectRemoteNodes(supplier, receiver, synchronise);
        } finally {
            if (threads > 0)
                structuralChangeLock.unlock();
        }
    }

    /**
     * Severs connection between a supplier and a receiver node, regardless which container they are in. Not to be
     * called remotely, because this method enforces the structural lock.
     *
     * @param supplier
     * @param receiver
     * @param desynchronise
     *            indicates whether the current contents of the supplier should be subtracted from the receiver
     */
    public void disconnectRemoteNodes(Address<? extends Supplier> supplier, Address<? extends Receiver> receiver,
            boolean desynchronise) {
        try {
            if (threads > 0)
                structuralChangeLock.lock();
            receiver.getContainer().disconnectRemoteNodes(supplier, receiver, desynchronise);
        } finally {
            if (threads > 0)
                structuralChangeLock.unlock();
        }
    }

    /**
     * Containers use this method to report whenever they run out of messages in their queue.
     *
     * To be called from the thread of the reporting container.
     *
     * @pre threads > 0.
     * @param reportingContainer
     *            the container reporting the emptiness of its message queue.
     * @param clock
     *            the value of the container's clock when reporting.
     * @param localTerminationCriteria
     *            the latest clock values this container has received from other containers since the last time it
     *            reported termination.
     */
    void reportLocalUpdateTermination(ReteContainer reportingContainer, long clock,
            Map<ReteContainer, Long> localTerminationCriteria) {
        synchronized (globalTerminationCriteria) {
            for (Entry<ReteContainer, Long> criterion : localTerminationCriteria.entrySet()) {
                terminationCriterion(criterion.getKey(), criterion.getValue());
            }

            reportedClocks.put(reportingContainer, clock);
            Long criterion = globalTerminationCriteria.get(reportingContainer);
            if (criterion != null && criterion < clock)
                globalTerminationCriteria.remove(reportingContainer);

            if (globalTerminationCriteria.isEmpty())
                globalTerminationCriteria.notifyAll();
        }
    }

    /**
     * @pre threads > 0
     */
    private void terminationCriterion(ReteContainer affectedContainer, long newCriterion) {
        synchronized (globalTerminationCriteria) {
            Long oldCriterion = globalTerminationCriteria.get(affectedContainer);
            Long oldClock = reportedClocks.get(affectedContainer);
            long relevantClock = oldClock == null ? 0 : oldClock;
            if ((relevantClock <= newCriterion) && (oldCriterion == null || oldCriterion < newCriterion)) {
                globalTerminationCriteria.put(affectedContainer, newCriterion);
            }
        }
    }

    /**
     * Waits until all rete update operations are settled in all containers. Returns immediately, if no updates are
     * pending.
     *
     * To be called from any user thread.
     */
    public void waitForReteTermination() {
        if (threads > 0) {
            synchronized (globalTerminationCriteria) {
                while (!globalTerminationCriteria.isEmpty()) {
                    try {
                        globalTerminationCriteria.wait();
                    } catch (InterruptedException e) {
						Thread.currentThread().interrupt();
                    }
                }
            }
        } else
            headContainer.deliverMessagesSingleThreaded();
    }

    /**
     * Waits to execute action until all rete update operations are settled in all containers. Runs action and returns
     * immediately, if no updates are pending. The given action is guaranteed to be run when the terminated state still
     * persists.
     *
     * @param action
     *            the action to be run when reaching the steady-state.
     *
     *            To be called from any user thread.
     */
    public void waitForReteTermination(Runnable action) {
        if (threads > 0) {
            synchronized (globalTerminationCriteria) {
                while (!globalTerminationCriteria.isEmpty()) {
                    try {
                        globalTerminationCriteria.wait();
                    } catch (InterruptedException e) {
						Thread.currentThread().interrupt();
                    }
                }
                action.run();
            }
        } else {
            headContainer.deliverMessagesSingleThreaded();
            action.run();
        }

    }

    /**
     * @return an unmodifiable set of known recipe traces
     */
    public Set<RecipeTraceInfo> getRecipeTraces() {
        return Collections.unmodifiableSet(recipeTraces);
    }

    /**
     * @return an unmodifiable list of containers
     */
    public List<ReteContainer> getContainers() {
        return Collections.unmodifiableList(containers);
    }

    public Lock getStructuralChangeLock() {
        return structuralChangeLock;
    }

    public NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    public InputConnector getInputConnector() {
        return inputConnector;
    }

    public ReteEngine getEngine() {
        return engine;
    }

}
