/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.jetbrains.annotations.NotNull;
import tools.refinery.store.query.literal.EquivalenceLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.literal.VariableDirection;
import tools.refinery.store.query.substitution.MapBasedSubstitution;
import tools.refinery.store.query.substitution.StatelessSubstitution;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class ClausePostProcessor {
	private final Set<Variable> parameters;
	private final Map<Variable, Integer> parameterWeights;
	private final List<Literal> literals;
	private final Map<NodeVariable, NodeVariable> representatives = new LinkedHashMap<>();
	private final Map<NodeVariable, Set<NodeVariable>> equivalencePartition = new HashMap<>();
	private List<Literal> substitutedLiterals;
	private final Set<Variable> existentiallyQuantifiedVariables = new LinkedHashSet<>();
	private Set<Variable> inputParameters;
	private Set<Variable> positiveVariables;
	private Map<Variable, Set<SortableLiteral>> variableToLiteralInputMap;
	private PriorityQueue<SortableLiteral> literalsWithAllInputsBound;
	private LinkedHashSet<Literal> topologicallySortedLiterals;

	public ClausePostProcessor(Set<Variable> parameters, Map<Variable, Integer> parameterWeights,
							   List<Literal> literals) {
		this.parameters = parameters;
		this.parameterWeights = parameterWeights;
		this.literals = literals;
	}

	public Result postProcessClause() {
		mergeEquivalentNodeVariables();
		substitutedLiterals = new ArrayList<>(literals.size());
		keepParameterEquivalences();
		substituteLiterals();
		computePositiveVariables();
		validatePositiveRepresentatives();
		validateClosureVariables();
		topologicallySortLiterals();
		var filteredLiterals = new ArrayList<Literal>(topologicallySortedLiterals.size());
		for (var literal : topologicallySortedLiterals) {
			var reduction = literal.getReduction();
			switch (reduction) {
			case NOT_REDUCIBLE -> filteredLiterals.add(literal);
			case ALWAYS_TRUE -> {
				// Literals reducible to {@code true} can be omitted, because the model is always assumed to have at
				// least on object.
			}
			case ALWAYS_FALSE -> {
				// Clauses with {@code false} literals can be omitted entirely.
				return ConstantResult.ALWAYS_FALSE;
			}
			default -> throw new IllegalArgumentException("Invalid reduction: " + reduction);
			}
		}
		if (filteredLiterals.isEmpty()) {
			return ConstantResult.ALWAYS_TRUE;
		}
		var clause = new DnfClause(Collections.unmodifiableSet(positiveVariables),
				Collections.unmodifiableList(filteredLiterals));
		return new ClauseResult(clause);
	}

	private void mergeEquivalentNodeVariables() {
		for (var literal : literals) {
			if (isPositiveEquivalence(literal)) {
				var equivalenceLiteral = (EquivalenceLiteral) literal;
				mergeVariables(equivalenceLiteral.getLeft(), equivalenceLiteral.getRight());
			}
		}
	}

	private static boolean isPositiveEquivalence(Literal literal) {
		return literal instanceof EquivalenceLiteral equivalenceLiteral && equivalenceLiteral.isPositive();
	}

	private void mergeVariables(NodeVariable left, NodeVariable right) {
		var leftRepresentative = getRepresentative(left);
		var rightRepresentative = getRepresentative(right);
		if (parameters.contains(leftRepresentative) && (!parameters.contains(rightRepresentative) ||
				parameterWeights.get(leftRepresentative).compareTo(parameterWeights.get(rightRepresentative)) <= 0)) {
			// Prefer the variable occurring earlier in the parameter list as a representative.
			doMergeVariables(leftRepresentative, rightRepresentative);
		} else {
			doMergeVariables(rightRepresentative, leftRepresentative);
		}
	}

	private void doMergeVariables(NodeVariable parentRepresentative, NodeVariable newChildRepresentative) {
		var parentSet = getEquivalentVariables(parentRepresentative);
		var childSet = getEquivalentVariables(newChildRepresentative);
		parentSet.addAll(childSet);
		equivalencePartition.remove(newChildRepresentative);
		for (var childEquivalentNodeVariable : childSet) {
			representatives.put(childEquivalentNodeVariable, parentRepresentative);
		}
	}

	private NodeVariable getRepresentative(NodeVariable variable) {
		return representatives.computeIfAbsent(variable, Function.identity());
	}

	private Set<NodeVariable> getEquivalentVariables(NodeVariable variable) {
		var representative = getRepresentative(variable);
		if (!representative.equals(variable)) {
			throw new IllegalStateException("NodeVariable %s already has a representative %s"
					.formatted(variable, representative));
		}
		return equivalencePartition.computeIfAbsent(variable, key -> {
			var set = new HashSet<NodeVariable>(1);
			set.add(key);
			return set;
		});
	}

	private void keepParameterEquivalences() {
		for (var pair : representatives.entrySet()) {
			var left = pair.getKey();
			var right = pair.getValue();
			if (!left.equals(right) && parameters.contains(left) && parameters.contains(right)) {
				substitutedLiterals.add(left.isEquivalent(right));
			}
		}
	}

	private void substituteLiterals() {
		var substitution = new MapBasedSubstitution(Collections.unmodifiableMap(representatives),
				StatelessSubstitution.IDENTITY);
		for (var literal : literals) {
			if (isPositiveEquivalence(literal)) {
				// We already retained all equivalences that cannot be replaced with substitutions in
				// {@link#keepParameterEquivalences()}.
				continue;
			}
			var substitutedLiteral = literal.substitute(substitution);
			substitutedLiterals.add(substitutedLiteral);
		}
	}

	private void computePositiveVariables() {
		for (var literal : substitutedLiterals) {
			var variableBinder = literal.getVariableBinder();
			variableBinder.getVariablesWithDirection(VariableDirection.IN_OUT)
					.forEach(existentiallyQuantifiedVariables::add);
			variableBinder.getVariablesWithDirection(VariableDirection.OUT).forEach(variable -> {
				boolean added = existentiallyQuantifiedVariables.add(variable);
				if (!added) {
					throw new IllegalArgumentException("Variable %s has multiple %s bindings"
							.formatted(variable, VariableDirection.OUT));
				}
			});
		}
		// Input parameters are parameters not bound by any positive literal.
		inputParameters = new LinkedHashSet<>(parameters);
		inputParameters.removeAll(existentiallyQuantifiedVariables);
		// Existentially quantified variables are variables appearing in positive literals that aren't parameters.
		existentiallyQuantifiedVariables.removeAll(parameters);
		// Positive variables are parameters (including input parameters) and variables bound by positive literals.
		positiveVariables = new LinkedHashSet<>(parameters.size() + existentiallyQuantifiedVariables.size());
		positiveVariables.addAll(parameters);
		positiveVariables.addAll(existentiallyQuantifiedVariables);
	}

	private void validatePositiveRepresentatives() {
		for (var pair : equivalencePartition.entrySet()) {
			var representative = pair.getKey();
			if (!positiveVariables.contains(representative)) {
				var variableSet = pair.getValue();
				throw new IllegalArgumentException("Variables %s were merged by equivalence but are not bound"
						.formatted(variableSet));
			}
		}
	}

	private void validateClosureVariables() {
		var negativeVariablesMap = new HashMap<Variable, Literal>();
		for (var literal : substitutedLiterals) {
			var variableBinder = literal.getVariableBinder();
			variableBinder.getVariablesWithDirection(VariableDirection.CLOSURE, positiveVariables)
					.forEach(variable -> {
						var oldLiteral = negativeVariablesMap.put(variable, literal);
						if (oldLiteral != null) {
							throw new IllegalArgumentException(
									"Unbound variable %s appears in multiple literals %s and %s"
											.formatted(variable, oldLiteral, literal));
						}
					});
		}
	}

	private void topologicallySortLiterals() {
		topologicallySortedLiterals = new LinkedHashSet<>(substitutedLiterals.size());
		variableToLiteralInputMap = new HashMap<>();
		literalsWithAllInputsBound = new PriorityQueue<>();
		int size = substitutedLiterals.size();
		for (int i = 0; i < size; i++) {
			var literal = substitutedLiterals.get(i);
			var sortableLiteral = new SortableLiteral(i, literal);
			sortableLiteral.enqueue();
		}
		while (!literalsWithAllInputsBound.isEmpty()) {
			var variable = literalsWithAllInputsBound.remove();
			variable.addToSortedLiterals();
		}
		if (!variableToLiteralInputMap.isEmpty()) {
			throw new IllegalArgumentException("Unbound input variables %s"
					.formatted(variableToLiteralInputMap.keySet()));
		}
	}

	private void topologicallySortVariable(Variable variable) {
		var literalSetForInput = variableToLiteralInputMap.remove(variable);
		if (literalSetForInput == null) {
			return;
		}
		for (var targetSortableLiteral : literalSetForInput) {
			targetSortableLiteral.bindVariable(variable);
		}
	}

	private class SortableLiteral implements Comparable<SortableLiteral> {
		private final int index;
		private final Literal literal;
		private final Set<Variable> remainingInputs;

		private SortableLiteral(int index, Literal literal) {
			this.index = index;
			this.literal = literal;
			remainingInputs = literal.getVariableBinder()
					.getVariablesWithDirection(VariableDirection.IN, positiveVariables)
					.collect(Collectors.toCollection(HashSet::new));
			remainingInputs.removeAll(inputParameters);
		}

		public void enqueue() {
			if (allInputsBound()) {
				addToAllInputsBoundQueue();
			} else {
				addToVariableToLiteralInputMap();
			}
		}

		private void bindVariable(Variable input) {
			if (!remainingInputs.remove(input)) {
				throw new IllegalStateException("Already processed input %s of literal %s".formatted(input, literal));
			}
			if (allInputsBound()) {
				addToAllInputsBoundQueue();
			}
		}

		private boolean allInputsBound() {
			return remainingInputs.isEmpty();
		}

		private void addToVariableToLiteralInputMap() {
			for (var inputVariable : remainingInputs) {
				var literalSetForInput = variableToLiteralInputMap.computeIfAbsent(
						inputVariable, key -> new HashSet<>());
				literalSetForInput.add(this);
			}
		}

		private void addToAllInputsBoundQueue() {
			literalsWithAllInputsBound.add(this);
		}

		public void addToSortedLiterals() {
			if (!allInputsBound()) {
				throw new AssertionError("Inputs %s of %s are not yet bound".formatted(remainingInputs, literal));
			}
			// Add literal if we haven't yet added a duplicate of this literal.
			topologicallySortedLiterals.add(literal);
			var variableBinder = literal.getVariableBinder();
			variableBinder.getVariablesWithDirection(VariableDirection.IN_OUT)
					.forEach(ClausePostProcessor.this::topologicallySortVariable);
			variableBinder.getVariablesWithDirection(VariableDirection.OUT)
					.forEach(ClausePostProcessor.this::topologicallySortVariable);
		}

		@Override
		public int compareTo(@NotNull ClausePostProcessor.SortableLiteral other) {
			return Integer.compare(index, other.index);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SortableLiteral that = (SortableLiteral) o;
			return index == that.index && Objects.equals(literal, that.literal);
		}

		@Override
		public int hashCode() {
			return Objects.hash(index, literal);
		}
	}

	public sealed interface Result permits ClauseResult, ConstantResult {
	}

	public record ClauseResult(DnfClause clause) implements Result {
	}

	public enum ConstantResult implements Result {
		ALWAYS_TRUE,
		ALWAYS_FALSE
	}
}
