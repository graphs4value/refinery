/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.base;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.MergeResult;
import tools.refinery.store.reasoning.PartialInterpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

public class BaseDecisionInterpretation implements PartialInterpretation<TruthValue, Boolean> {
	private final ReasoningAdapter reasoningAdapter;
	private PartialRelation partialRelation;
	private final ResultSet<Boolean> mustResultSet;
	private final ResultSet<Boolean> mayResultSet;
	private final ResultSet<Boolean> errorResultSet;
	private final ResultSet<Boolean> currentResultSet;
	private final Interpretation<TruthValue> interpretation;

	public BaseDecisionInterpretation(ReasoningAdapter reasoningAdapter, ResultSet<Boolean> mustResultSet,
									  ResultSet<Boolean> mayResultSet, ResultSet<Boolean> errorResultSet,
									  ResultSet<Boolean> currentResultSet, Interpretation<TruthValue> interpretation) {
		this.reasoningAdapter = reasoningAdapter;
		this.mustResultSet = mustResultSet;
		this.mayResultSet = mayResultSet;
		this.errorResultSet = errorResultSet;
		this.currentResultSet = currentResultSet;
		this.interpretation = interpretation;
	}

	@Override
	public ReasoningAdapter getAdapter() {
		return reasoningAdapter;
	}

	@Override
	public int countUnfinished() {
		return 0;
	}

	@Override
	public int countErrors() {
		return errorResultSet.size();
	}

	@Override
	public PartialRelation getPartialSymbol() {
		return partialRelation;
	}

	@Override
	public TruthValue get(Tuple key) {
		return null;
	}

	@Override
	public Cursor<Tuple, TruthValue> getAll() {
		return null;
	}

	@Override
	public MergeResult merge(Tuple key, TruthValue value) {
		TruthValue newValue;
		switch (value) {
		case UNKNOWN -> {
			return MergeResult.UNCHANGED;
		}
		case TRUE -> newValue = mayResultSet.get(key) ? TruthValue.TRUE : TruthValue.ERROR;
		case FALSE -> newValue = mustResultSet.get(key) ? TruthValue.ERROR : TruthValue.FALSE;
		case ERROR -> newValue = TruthValue.ERROR;
		default -> throw new IllegalArgumentException("Unknown truth value: " + value);
		}
		var oldValue = interpretation.put(key, newValue);
		return oldValue == TruthValue.ERROR ? MergeResult.UNCHANGED : MergeResult.REFINED;
	}

	@Override
	public Boolean getConcrete(Tuple key) {
		return currentResultSet.get(key);
	}

	@Override
	public Cursor<Tuple, Boolean> getAllConcrete() {
		return null;
	}
}
