/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.substitution;

import tools.refinery.logic.term.Variable;

import java.util.Map;

public record MapBasedSubstitution(Map<Variable, Variable> map, Substitution fallback) implements Substitution {
	@Override
	public Variable getSubstitute(Variable variable) {
		var value = map.get(variable);
		return value == null ? fallback.getSubstitute(variable) : value;
	}
}
