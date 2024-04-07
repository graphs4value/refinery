/*******************************************************************************
 * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.timeline.Timeline;
import tools.refinery.interpreter.rete.index.DefaultIndexerListener;
import tools.refinery.interpreter.rete.index.Indexer;
import tools.refinery.interpreter.rete.index.ProjectionIndexer;
import tools.refinery.interpreter.rete.index.StandardIndexer;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ReteContainer;
import tools.refinery.interpreter.rete.network.StandardNode;
import tools.refinery.interpreter.rete.network.communication.Timestamp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LeftJoinNode extends StandardNode {
	private final Object defaultValue;
	private ProjectionIndexer projectionIndexer;
	private TupleMask projectionMask;
	private boolean leftInheritanceOutputMask;
	private OuterIndexer outerIndexer = null;

	public LeftJoinNode(ReteContainer reteContainer, Object defaultValue) {
		super(reteContainer);
		this.defaultValue = defaultValue;
	}

	@Override
	public void networkStructureChanged() {
		if (reteContainer.isTimelyEvaluation() && reteContainer.getCommunicationTracker().isInRecursiveGroup(this)) {
			throw new IllegalStateException(this + " cannot be used in recursive differential dataflow evaluation!");
		}
		super.networkStructureChanged();
	}

	public void initializeWith(ProjectionIndexer projectionIndexer) {
		this.projectionIndexer = projectionIndexer;
		projectionMask = projectionIndexer.getMask();
		leftInheritanceOutputMask = isLeftInheritanceOutputMask(projectionMask);
		projectionIndexer.attachListener(new DefaultIndexerListener(this) {
			@Override
			public void notifyIndexerUpdate(Direction direction, Tuple updateElement, Tuple signature, boolean change,
											Timestamp timestamp) {
				update(direction, updateElement, signature, change, timestamp);
			}
		});
	}

	private static boolean isLeftInheritanceOutputMask(TupleMask mask) {
		int size = mask.getSize();
		int sourceWidth = mask.getSourceWidth();
		if (size != sourceWidth - 1) {
			throw new IllegalArgumentException("projectionMask should omit a single index, got " + mask);
		}
		int[] repetitions = new int[sourceWidth];
		for (int i = 0; i < size; i++) {
			int index = mask.indices[i];
			int repetition = repetitions[index] + 1;
			if (repetition >= 2) {
				throw new IllegalArgumentException("Repeated index %d in projectionMask %s".formatted(index, mask));
			}
			repetitions[index] = repetition;
		}
		for (int i = 0; i < size; i++) {
			int index = mask.indices[i];
			if (index != i) {
				return false;
			}
		}
		return true;
	}

	protected void update(Direction direction, Tuple updateElement, Tuple signature, boolean change,
						  Timestamp timestamp) {
		propagateUpdate(direction, updateElement, timestamp);
		if (outerIndexer != null) {
			outerIndexer.update(direction, updateElement, signature, change, timestamp);
		}
	}

	protected Tuple getDefaultTuple(ITuple key) {
		if (leftInheritanceOutputMask) {
			return Tuples.staticArityFlatTupleOf(key, defaultValue);
		}
		var objects = new Object[projectionMask.sourceWidth];
		int targetLength = projectionMask.indices.length;
		for (int i = 0; i < targetLength; i++) {
			int j = projectionMask.indices[i];
			objects[j] = key.get(j);
		}
		return Tuples.flatTupleOf(objects);
	}

	@Override
	public void pullInto(Collection<Tuple> collector, boolean flush) {
		projectionIndexer.getParent().pullInto(collector, flush);
	}

	@Override
	public void pullIntoWithTimeline(Map<Tuple, Timeline<Timestamp>> collector, boolean flush) {
		projectionIndexer.getParent().pullIntoWithTimeline(collector, flush);
	}

	@Override
	public Set<Tuple> getPulledContents(boolean flush) {
		return projectionIndexer.getParent().getPulledContents(flush);
	}

	public Indexer getOuterIndexer() {
		if (outerIndexer == null) {
			outerIndexer = new OuterIndexer();
			getCommunicationTracker().registerDependency(this, outerIndexer);
		}
		return outerIndexer;
	}

	/**
	 * A special non-iterable index that retrieves the aggregated, packed result (signature+aggregate) for the original
	 * signature.
	 *
	 * @author Gabor Bergmann
	 */
	class OuterIndexer extends StandardIndexer {
		public OuterIndexer() {
			super(LeftJoinNode.this.reteContainer, LeftJoinNode.this.projectionMask);
			this.parent = LeftJoinNode.this;
		}

		@Override
		public Collection<Tuple> get(Tuple signature) {
			var collection = projectionIndexer.get(signature);
			if (collection == null || collection.isEmpty()) {
				return List.of(getDefaultTuple(signature));
			}
			return collection;
		}

		public void update(Direction direction, Tuple updateElement, Tuple signature, boolean change,
						   Timestamp timestamp) {
			propagate(direction, updateElement, signature, false, timestamp);
			if (change) {
				var defaultTuple = getDefaultTuple(signature);
				propagate(direction.opposite(), defaultTuple, signature, false, timestamp);
			}
		}

		@Override
		public Node getActiveNode() {
			return projectionIndexer.getActiveNode();
		}
	}
}
