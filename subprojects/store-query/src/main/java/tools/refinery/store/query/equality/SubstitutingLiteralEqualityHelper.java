/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.equality;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.SymbolicParameter;
import tools.refinery.store.query.term.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubstitutingLiteralEqualityHelper implements LiteralEqualityHelper {
	private final DnfEqualityChecker dnfEqualityChecker;
	private final Map<Variable, Variable> leftToRight;
	private final Map<Variable, Variable> rightToLeft;

	public SubstitutingLiteralEqualityHelper(DnfEqualityChecker dnfEqualityChecker,
											 List<SymbolicParameter> leftParameters,
											 List<SymbolicParameter> rightParameters) {
		this.dnfEqualityChecker = dnfEqualityChecker;
		var arity = leftParameters.size();
		if (arity != rightParameters.size()) {
			throw new IllegalArgumentException("Parameter lists have unequal length");
		}
		leftToRight = new HashMap<>(arity);
		rightToLeft = new HashMap<>(arity);
		for (int i = 0; i < arity; i++) {
			if (!variableEqual(leftParameters.get(i).getVariable(), rightParameters.get(i).getVariable())) {
				throw new IllegalArgumentException("Parameter lists cannot be unified: duplicate parameter " + i);
			}
		}
	}

	@Override
	public boolean dnfEqual(Dnf left, Dnf right) {
		return dnfEqualityChecker.dnfEqual(left, right);
	}

	@Override
	public boolean variableEqual(Variable left, Variable right) {
		if (left.tryGetType().equals(right.tryGetType()) &&
				checkMapping(leftToRight, left, right) &&
				checkMapping(rightToLeft, right, left)) {
			leftToRight.put(left, right);
			rightToLeft.put(right, left);
			return true;
		}
		return false;
	}

	private static boolean checkMapping(Map<Variable, Variable> map, Variable key, Variable expectedValue) {
		var currentValue = map.get(key);
		return currentValue == null || currentValue.equals(expectedValue);
	}
}
