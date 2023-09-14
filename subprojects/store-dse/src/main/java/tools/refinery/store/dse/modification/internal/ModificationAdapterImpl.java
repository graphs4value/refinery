/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification.internal;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.dse.modification.DanglingEdges;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.util.HashSet;

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
	public boolean deleteObject(Tuple tuple, DanglingEdges danglingEdges) {
		if (tuple.getSize() != 1) {
			throw new IllegalArgumentException("Tuple size must be 1");
		}
		int objectId = tuple.get(0);
		if (danglingEdges == DanglingEdges.DELETE) {
			deleteDanglingEdges(objectId);
		} else if (danglingEdges == DanglingEdges.FAIL && hasDanglingEdges(objectId)) {
			return false;

		}
		int modelSize = getModelSize();
		if (objectId == modelSize - 1) {
			nodeCountInterpretation.put(Tuple.of(), modelSize - 1);
		}
		return true;
	}

	private void deleteDanglingEdges(int objectId) {
		for (var symbol : model.getStore().getSymbols()) {
			deleteDanglingEdges(objectId, (Symbol<?>) symbol);
		}
	}

	private <T> void deleteDanglingEdges(int objectId, Symbol<T> symbol) {
		var interpretation = model.getInterpretation(symbol);
		var toDelete = new HashSet<Tuple>();
		int arity = symbol.arity();
		for (int i = 0; i < arity; i++) {
			var cursor = interpretation.getAdjacent(i, objectId);
			while (cursor.move()) {
				toDelete.add(cursor.getKey());
			}
		}
		var defaultValue = symbol.defaultValue();
		for (var tuple : toDelete) {
			interpretation.put(tuple, defaultValue);
		}
	}

	private boolean hasDanglingEdges(int objectId) {
		for (var symbol : model.getStore().getSymbols()) {
			var interpretation = model.getInterpretation(symbol);
			int arity = symbol.arity();
			for (int i = 0; i < arity; i++) {
				if (interpretation.getAdjacentSize(i, objectId) > 0) {
					return true;
				}
			}
		}
		return false;
	}
}
