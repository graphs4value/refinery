/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.actions;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.dse.transition.actions.AbstractActionLiteral;
import tools.refinery.store.dse.transition.actions.BoundActionLiteral;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.LinkedHashMap;
import java.util.List;

public class ComputedMergeActionLiteral<A extends AbstractValue<A, C>, C> extends AbstractActionLiteral {
	private final PartialSymbol<A, C> partialSymbol;
	private final List<NodeVariable> parameters;
	private final List<NodeVariable> arguments;
	private final List<NodeVariable> inputVariables;
	private final int @Nullable [] parameterMapping;
	private final int @Nullable [] argumentMapping;
	private final FunctionalQuery<A> valueQuery;

	public ComputedMergeActionLiteral(PartialSymbol<A, C> partialSymbol, List<NodeVariable> parameters,
									  FunctionalQuery<A> valueQuery, List<NodeVariable> arguments) {
		if (partialSymbol.arity() != parameters.size()) {
			throw new IllegalArgumentException("Expected %d parameters for partial symbol %s, got %d instead"
					.formatted(partialSymbol.arity(), partialSymbol, parameters.size()));
		}
		if (valueQuery.arity() != arguments.size()) {
			throw new IllegalArgumentException("Expected %d arguments for query %s, got %d instead"
					.formatted(valueQuery.arity(), valueQuery, arguments.size()));
		}
		this.partialSymbol = partialSymbol;
		this.parameters = parameters;
		this.arguments = arguments;
		this.valueQuery = valueQuery;
		var allocation = new LinkedHashMap<NodeVariable, Integer>();
		var theParameterMapping = mapVariables(parameters, allocation);
		var theArgumentMapping = mapVariables(arguments, allocation);
		inputVariables = List.copyOf(allocation.sequencedKeySet());
		parameterMapping = isIdentity(theParameterMapping) ? null : theParameterMapping;
		argumentMapping = isIdentity(theArgumentMapping) ? null : theArgumentMapping;
	}

	private static int[] mapVariables(List<NodeVariable> variables, LinkedHashMap<NodeVariable, Integer> allocation) {
		int size = variables.size();
		var mapping = new int[size];
		for (int i = 0; i < size; i++) {
			mapping[i] = allocation.computeIfAbsent(variables.get(i), ignored -> allocation.size());
		}
		return mapping;
	}

	private boolean isIdentity(int[] mapping) {
		int length = mapping.length;
		if (length != inputVariables.size()) {
			return false;
		}
		for (int i = 0; i < length; i++) {
			if (mapping[i] != i) {
				return false;
			}
		}
		return true;
	}

	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}

	public List<NodeVariable> getParameters() {
		return parameters;
	}

	public List<NodeVariable> getArguments() {
		return arguments;
	}

	public FunctionalQuery<A> getValueQuery() {
		return valueQuery;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return inputVariables;
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of();
	}

	@Override
	public List<AnyQuery> getQueries() {
		return List.of(valueQuery);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var refiner = model.getAdapter(ReasoningAdapter.class).getRefiner(partialSymbol);
		var resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(valueQuery);
		return tuple -> {
			var value = resultSet.get(mapTuple(argumentMapping, tuple));
			if (value == null) {
				return null;
			}
			return refiner.merge(mapTuple(parameterMapping, tuple), value) ? Tuple.of() : null;
		};
	}

	private static Tuple mapTuple(int @Nullable [] mapping, Tuple tuple) {
		if (mapping == null) {
			return tuple;
		}
		int length = mapping.length;
		var values = new int[length];
		for (int i = 0; i < length; i++) {
			values[i] = tuple.get(mapping[i]);
		}
		return Tuple.of(values);
	}
}
