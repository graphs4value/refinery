/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.substitution;

import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;

@FunctionalInterface
public interface Substitution {
	Variable getSubstitute(Variable variable);

	default NodeVariable getTypeSafeSubstitute(NodeVariable variable) {
		var substitute = getSubstitute(variable);
		return substitute.asNodeVariable();
	}

	default <T> DataVariable<T> getTypeSafeSubstitute(DataVariable<T> variable) {
		var substitute = getSubstitute(variable);
		return substitute.asDataVariable(variable.getType());
	}

	static SubstitutionBuilder builder() {
		return new SubstitutionBuilder();
	}
}
