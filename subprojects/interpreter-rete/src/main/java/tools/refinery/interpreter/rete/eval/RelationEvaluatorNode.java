/*******************************************************************************
 * Copyright (c) 2010-2022, Tamas Szabo, GitHub
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.interpreter.rete.misc.SimpleReceiver;
import tools.refinery.interpreter.rete.network.communication.Timestamp;
import tools.refinery.interpreter.rete.single.AbstractUniquenessEnforcerNode;
import tools.refinery.interpreter.matchers.psystem.IRelationEvaluator;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.RelationEvaluation;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Clearable;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.matchers.util.timeline.Timelines;
import tools.refinery.interpreter.rete.network.ProductionNode;
import tools.refinery.interpreter.rete.network.Receiver;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.StandardNode;
import tools.refinery.interpreter.rete.network.Supplier;

/**
 * A node that operates in batch-style (see {@link Receiver#doesProcessUpdatesInBatch()} and evaluates arbitrary Java
 * logic represented by an {@link IRelationEvaluator} on the input relations. This is the backing computation node of a
 * {@link RelationEvaluation} constraint.
 *
 * @author Tamas Szabo
 * @since 2.8
 */
public class RelationEvaluatorNode extends StandardNode implements Supplier, Clearable {

    private final IRelationEvaluator evaluator;
    private Set<Tuple> cachedOutputs;
    private Supplier[] inputSuppliers;
    private BatchingReceiver[] inputReceivers;

    public RelationEvaluatorNode(final ReteContainer container, final IRelationEvaluator evaluator) {
        super(container);
        this.evaluator = evaluator;
        this.reteContainer.registerClearable(this);
    }

    @Override
    public void clear() {
        this.cachedOutputs.clear();
    }

    public void connectToParents(final List<Supplier> inputSuppliers) {
        this.inputSuppliers = new Supplier[inputSuppliers.size()];
        this.inputReceivers = new BatchingReceiver[inputSuppliers.size()];

        final List<Integer> inputArities = evaluator.getInputArities();

        if (inputArities.size() != inputSuppliers.size()) {
            throw new IllegalStateException(evaluator.toString() + " expects " + inputArities.size()
                    + " inputs, but got " + inputSuppliers.size() + " input(s)!");
        }

        for (int i = 0; i < inputSuppliers.size(); i++) {
            final int currentExpectedInputArity = inputArities.get(i);
            final Supplier inputSupplier = inputSuppliers.get(i);
            // it is expected that the supplier is a production node because
            // the corresponding constraint itself accepts a list of PQuery
            if (!(inputSupplier instanceof ProductionNode)) {
                throw new IllegalStateException(
                        evaluator.toString() + " expects each one of its suppliers to be instances of "
                                + ProductionNode.class.getSimpleName() + " but got an instance of "
                                + inputSupplier.getClass().getSimpleName() + "!");
            }
            final int currentActualInputArity = ((ProductionNode) inputSupplier).getPosMapping().size();
            if (currentActualInputArity != currentExpectedInputArity) {
                throw new IllegalStateException(
                        evaluator.toString() + " expects input arity " + currentExpectedInputArity + " at position " + i
                                + " but got " + currentActualInputArity + "!");
            }
            final BatchingReceiver inputReceiver = new BatchingReceiver((ProductionNode) inputSupplier,
                    this.reteContainer);
            this.inputSuppliers[i] = inputSupplier;
            this.inputReceivers[i] = inputReceiver;
            this.reteContainer.connectAndSynchronize(inputSupplier, inputReceiver);
            reteContainer.getCommunicationTracker().registerDependency(inputReceiver, this);
        }

        // initialize the output relation
        final List<Set<Tuple>> inputSets = new ArrayList<Set<Tuple>>();
        for (final BatchingReceiver inputReceiver : this.inputReceivers) {
            inputSets.add(inputReceiver.getTuples());
        }
        this.cachedOutputs = evaluateRelation(inputSets);
    }

    @Override
    public void networkStructureChanged() {
        if (this.reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
            throw new IllegalStateException(this.toString() + " cannot be used in recursive evaluation!");
        }
        super.networkStructureChanged();
    }

    @Override
    public void pullInto(final Collection<Tuple> collector, final boolean flush) {
        collector.addAll(this.cachedOutputs);
    }

    @Override
    public void pullIntoWithTimeline(final Map<Tuple, Timeline<Timestamp>> collector, final boolean flush) {
        final Timeline<Timestamp> timeline = Timelines.createFrom(Timestamp.ZERO);
        for (final Tuple output : this.cachedOutputs) {
            collector.put(output, timeline);
        }
    }

    private Set<Tuple> evaluateRelation(final List<Set<Tuple>> inputs) {
        try {
            return this.evaluator.evaluateRelation(inputs);
        } catch (final Exception e) {
            throw new IllegalStateException("Exception during the evaluation of " + this.evaluator.toString() + "!", e);
        }
    }

    private void batchUpdateCompleted() {
        final List<Set<Tuple>> inputSets = new ArrayList<Set<Tuple>>();
        for (final BatchingReceiver inputReceiver : this.inputReceivers) {
            inputSets.add(inputReceiver.getTuples());
        }
        final Set<Tuple> newOutputs = evaluateRelation(inputSets);
        for (final Tuple tuple : newOutputs) {
            if (this.cachedOutputs != null && this.cachedOutputs.remove(tuple)) {
                // already known tuple - do nothing
            } else {
                // newly inserted tuple
                propagateUpdate(Direction.INSERT, tuple, Timestamp.ZERO);
            }
        }
        if (this.cachedOutputs != null) {
            for (final Tuple tuple : this.cachedOutputs) {
                // lost tuple
                propagateUpdate(Direction.DELETE, tuple, Timestamp.ZERO);
            }
        }
        this.cachedOutputs = newOutputs;
    }

    public class BatchingReceiver extends SimpleReceiver {
        private final ProductionNode source;

        private BatchingReceiver(final ProductionNode source, final ReteContainer container) {
            super(container);
            this.source = source;
        }

        private Set<Tuple> getTuples() {
            return ((AbstractUniquenessEnforcerNode) this.source).getTuples();
        }

        @Override
        public void update(final Direction direction, final Tuple updateElement, final Timestamp timestamp) {
            throw new UnsupportedOperationException("This receiver only supports batch-style operation!");
        }

        @Override
        public void batchUpdate(final Collection<Entry<Tuple, Integer>> updates, final Timestamp timestamp) {
            assert Timestamp.ZERO.equals(timestamp);
            // there is nothing to do here because the source production node has already updated itself
            // the only thing we need to do is to issue the callback
            RelationEvaluatorNode.this.batchUpdateCompleted();
        }

    }

}
