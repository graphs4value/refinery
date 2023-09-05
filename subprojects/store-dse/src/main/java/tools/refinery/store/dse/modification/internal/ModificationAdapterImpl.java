/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification.internal;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

public class ModificationAdapterImpl implements ModificationAdapter {
	static final Symbol<Integer> NEXT_ID = Symbol.of("NEXT_ID", 0, Integer.class, 0);

	final ModelStoreAdapter storeAdapter;
	final Model model;
	Interpretation<Integer> nodeCountInterpretation;

	ModificationAdapterImpl(ModelStoreAdapter storeAdapter, Model model) {
		this.storeAdapter = storeAdapter;
		this.model = model;
		this.nodeCountInterpretation = model.getInterpretation(NEXT_ID);
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ModelStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public int getModelSize() {
		return nodeCountInterpretation.get(Tuple.of());
	}

	@Override
	public Tuple1 createObject() {
		var newNodeId = getModelSize();
		nodeCountInterpretation.put(Tuple.of(), newNodeId + 1);
		return Tuple.of(newNodeId);
	}

	@Override
	public Tuple deleteObject(Tuple tuple) {
		if (tuple.getSize() != 1) {
			throw new IllegalArgumentException("Tuple size must be 1");
		}
//		TODO: implement more efficient deletion
		if (tuple.get(0) == getModelSize() - 1) {
			nodeCountInterpretation.put(Tuple.of(), getModelSize() - 1);
		}
		return tuple;
	}
}
