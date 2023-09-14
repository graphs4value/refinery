/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.equality;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.dnf.SymbolicParameter;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.util.CycleDetectingMapper;

import java.util.List;

public class DeepDnfEqualityChecker implements DnfEqualityChecker {
	private final CycleDetectingMapper<Pair, Boolean> mapper = new CycleDetectingMapper<>(this::doCheckEqual);

	@Override
	public boolean dnfEqual(Dnf left, Dnf right) {
		return mapper.map(new Pair(left, right));
	}

	public boolean dnfEqualRaw(List<SymbolicParameter> symbolicParameters,
							   List<? extends List<? extends Literal>> clauses, Dnf other) {
		int arity = symbolicParameters.size();
		if (arity != other.arity()) {
			return false;
		}
		for (int i = 0; i < arity; i++) {
			if (!symbolicParameters.get(i).getDirection().equals(other.getSymbolicParameters().get(i).getDirection())) {
				return false;
			}
		}
		int numClauses = clauses.size();
		if (numClauses != other.getClauses().size()) {
			return false;
		}
		for (int i = 0; i < numClauses; i++) {
			var literalEqualityHelper = new SubstitutingLiteralEqualityHelper(this, symbolicParameters,
					other.getSymbolicParameters());
			if (!equalsWithSubstitutionRaw(literalEqualityHelper, clauses.get(i), other.getClauses().get(i))) {
				return false;
			}
		}
		return true;
	}

	private boolean equalsWithSubstitutionRaw(LiteralEqualityHelper helper, List<? extends Literal> literals,
											  DnfClause other) {
		int size = literals.size();
		if (size != other.literals().size()) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			if (!literals.get(i).equalsWithSubstitution(helper, other.literals().get(i))) {
				return false;
			}
		}
		return true;
	}

	protected boolean doCheckEqual(Pair pair) {
		return pair.left.equalsWithSubstitution(this, pair.right);
	}

	protected List<Pair> getInProgress() {
		return mapper.getInProgress();
	}

	protected record Pair(Dnf left, Dnf right) {
		@Override
		public String toString() {
			return "(%s, %s)".formatted(left.name(), right.name());
		}
	}
}
