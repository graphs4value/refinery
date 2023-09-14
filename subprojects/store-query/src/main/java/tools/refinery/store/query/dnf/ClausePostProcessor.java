/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import org.jetbrains.annotations.NotNull;
import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.substitution.MapBasedSubstitution;
import tools.refinery.store.query.substitution.StatelessSubstitution;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.*;
import java.util.function.Function;

class ClausePostProcessor {
	private final Map<Variable, ParameterInfo> parameters;
	private final List<Literal> literals;
	private final Map<Variable, Variable> representatives = new LinkedHashMap<>();
	private final Map<Variable, Set<Variable>> equivalencePartition = new HashMap<>();
	private List<Literal> substitutedLiterals;
	private final Set<Variable> existentiallyQuantifiedVariables = new LinkedHashSet<>();
	private Set<Variable> positiveVariables;
	private Map<Variable, Set<SortableLiteral>> variableToLiteralInputMap;
	private PriorityQueue<SortableLiteral> literalsWithAllInputsBound;
	private LinkedHashSet<Literal> topologicallySortedLiterals;

	public ClausePostProcessor(Map<Variable, ParameterInfo> parameters, List<Literal> literals) {
		this.parameters = parameters;
		this.literals = literals;
	}

	public Result postProcessClause() {
		mergeEquivalentNodeVariables();
		substitutedLiterals = new ArrayList<>(literals.size());
		keepParameterEquivalences();
		substituteLiterals();
		computeExistentiallyQuantifiedVariables();
		computePositiveVariables();
		validatePositiveRepresentatives();
		validatePrivateVariables();
		topologicallySortLiterals();
		var filteredLiterals = new ArrayList<Literal>(topologicallySortedLiterals.size());
		for (var literal : topologicallySortedLiterals) {
			var reducedLiteral = literal.reduce();
			if (BooleanLiteral.FALSE.equals(reducedLiteral)) {
				return ConstantResult.ALWAYS_FALSE;
			} else if (!BooleanLiteral.TRUE.equals(reducedLiteral)) {
				filteredLiterals.add(reducedLiteral);
			}
		}
		if (filteredLiterals.isEmpty()) {
			return ConstantResult.ALWAYS_TRUE;
		}
		if (hasContradictoryCall(filteredLiterals)) {
			return ConstantResult.ALWAYS_FALSE;
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

	private void mergeVariables(Variable left, Variable right) {
		var leftRepresentative = getRepresentative(left);
		var rightRepresentative = getRepresentative(right);
		var leftInfo = parameters.get(leftRepresentative);
		var rightInfo = parameters.get(rightRepresentative);
		if (leftInfo != null && (rightInfo == null || leftInfo.index() <= rightInfo.index())) {
			// Prefer the variable occurring earlier in the parameter list as a representative.
			doMergeVariables(leftRepresentative, rightRepresentative);
		} else {
			doMergeVariables(rightRepresentative, leftRepresentative);
		}
	}

	private void doMergeVariables(Variable parentRepresentative, Variable newChildRepresentative) {
		var parentSet = getEquivalentVariables(parentRepresentative);
		var childSet = getEquivalentVariables(newChildRepresentative);
		parentSet.addAll(childSet);
		equivalencePartition.remove(newChildRepresentative);
		for (var childEquivalentNodeVariable : childSet) {
			representatives.put(childEquivalentNodeVariable, parentRepresentative);
		}
	}

	private Variable getRepresentative(Variable variable) {
		return representatives.computeIfAbsent(variable, Function.identity());
	}

	private Set<Variable> getEquivalentVariables(Variable variable) {
		var representative = getRepresentative(variable);
		if (!representative.equals(variable)) {
			throw new AssertionError("NodeVariable %s already has a representative %s"
					.formatted(variable, representative));
		}
		return equivalencePartition.computeIfAbsent(variable, key -> {
			var set = new HashSet<Variable>(1);
			set.add(key);
			return set;
		});
	}

	private void keepParameterEquivalences() {
		for (var pair : representatives.entrySet()) {
			var left = pair.getKey();
			var right = pair.getValue();
			if (!left.equals(right) && parameters.containsKey(left) && parameters.containsKey(right)) {
				substitutedLiterals.add(new EquivalenceLiteral(true, left, right));
			}
		}
	}

	private void substituteLiterals() {
		Substitution substitution;
		if (representatives.isEmpty()) {
			substitution = null;
		} else {
			substitution = new MapBasedSubstitution(Collections.unmodifiableMap(representatives),
					StatelessSubstitution.IDENTITY);
		}
		for (var literal : literals) {
			if (isPositiveEquivalence(literal)) {
				// We already retained all equivalences that cannot be replaced with substitutions in
				// {@link#keepParameterEquivalences()}.
				continue;
			}
			var substitutedLiteral = substitution == null ? literal : literal.substitute(substitution);
			substitutedLiterals.add(substitutedLiteral);
		}
	}

	private void computeExistentiallyQuantifiedVariables() {
		for (var literal : substitutedLiterals) {
			existentiallyQuantifiedVariables.addAll(literal.getOutputVariables());
		}
	}

	private void computePositiveVariables() {
		positiveVariables = new LinkedHashSet<>();
		for (var pair : parameters.entrySet()) {
			var variable = pair.getKey();
			if (pair.getValue().direction() == ParameterDirection.IN) {
				// Inputs count as positive, because they are already bound when we evaluate literals.
				positiveVariables.add(variable);
			} else if (!existentiallyQuantifiedVariables.contains(variable)) {
				throw new InvalidQueryException("Unbound %s parameter %s"
						.formatted(ParameterDirection.OUT, variable));
			}
		}
		positiveVariables.addAll(existentiallyQuantifiedVariables);
	}

	private void validatePositiveRepresentatives() {
		for (var pair : equivalencePartition.entrySet()) {
			var representative = pair.getKey();
			if (!positiveVariables.contains(representative)) {
				var variableSet = pair.getValue();
				throw new InvalidQueryException("Variables %s were merged by equivalence but are not bound"
						.formatted(variableSet));
			}
		}
	}

	private void validatePrivateVariables() {
		var negativeVariablesMap = new HashMap<Variable, Literal>();
		for (var literal : substitutedLiterals) {
			for (var variable : literal.getPrivateVariables(positiveVariables)) {
				var oldLiteral = negativeVariablesMap.put(variable, literal);
				if (oldLiteral != null) {
					throw new InvalidQueryException("Unbound variable %s appears in multiple literals %s and %s"
							.formatted(variable, oldLiteral, literal));
				}
			}
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
			throw new InvalidQueryException("Unbound input variables %s"
					.formatted(variableToLiteralInputMap.keySet()));
		}
	}

	private boolean hasContradictoryCall(Collection<Literal> filteredLiterals) {
		var positiveCalls = new HashMap<Constraint, Set<CallLiteral>>();
		for (var literal : filteredLiterals) {
			if (literal instanceof CallLiteral callLiteral && callLiteral.getPolarity() == CallPolarity.POSITIVE) {
				var callsOfTarget = positiveCalls.computeIfAbsent(callLiteral.getTarget(), key -> new HashSet<>());
				callsOfTarget.add(callLiteral);
			}
		}
		for (var literal : filteredLiterals) {
			if (literal instanceof CallLiteral callLiteral && callLiteral.getPolarity() == CallPolarity.NEGATIVE) {
				var callsOfTarget = positiveCalls.get(callLiteral.getTarget());
				if (contradicts(callLiteral, callsOfTarget)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean contradicts(CallLiteral negativeCall, Collection<CallLiteral> positiveCalls) {
		if (positiveCalls == null) {
			return false;
		}
		for (var positiveCall : positiveCalls) {
			if (contradicts(negativeCall, positiveCall)) {
				return true;
			}
		}
		return false;
	}

	private boolean contradicts(CallLiteral negativeCall, CallLiteral positiveCall) {
		var privateVariables = negativeCall.getPrivateVariables(positiveVariables);
		var negativeArguments = negativeCall.getArguments();
		var positiveArguments = positiveCall.getArguments();
		int arity = negativeArguments.size();
		for (int i = 0; i < arity; i++) {
			var negativeArgument = negativeArguments.get(i);
			if (privateVariables.contains(negativeArgument)) {
				continue;
			}
			var positiveArgument = positiveArguments.get(i);
			if (!negativeArgument.equals(positiveArgument)) {
				return false;
			}
		}
		return true;
	}

	private class SortableLiteral implements Comparable<SortableLiteral> {
		private final int index;
		private final Literal literal;
		private final Set<Variable> remainingInputs;

		private SortableLiteral(int index, Literal literal) {
			this.index = index;
			this.literal = literal;
			remainingInputs = new HashSet<>(literal.getInputVariables(positiveVariables));
			for (var pair : parameters.entrySet()) {
				if (pair.getValue().direction() == ParameterDirection.IN) {
					remainingInputs.remove(pair.getKey());
				}
			}
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
				throw new AssertionError("Already processed input %s of literal %s".formatted(input, literal));
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
			for (var variable : literal.getOutputVariables()) {
				var literalSetForInput = variableToLiteralInputMap.remove(variable);
				if (literalSetForInput == null) {
					continue;
				}
				for (var targetSortableLiteral : literalSetForInput) {
					targetSortableLiteral.bindVariable(variable);
				}
			}
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

	public record ParameterInfo(ParameterDirection direction, int index) {
	}
}
