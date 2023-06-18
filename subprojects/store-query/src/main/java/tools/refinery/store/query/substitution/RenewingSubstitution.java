/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.substitution;

import tools.refinery.store.query.term.Variable;

import java.util.HashMap;
import java.util.Map;

public class RenewingSubstitution implements Substitution {
	private final Map<Variable, Variable> alreadyRenewed = new HashMap<>();

	@Override
	public Variable getSubstitute(Variable variable) {
		return alreadyRenewed.computeIfAbsent(variable, Variable::renew);
	}
}
