/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.substitution;

import tools.refinery.store.query.term.Variable;

public record CompositeSubstitution(Substitution first, Substitution second) implements Substitution {
	@Override
	public Variable getSubstitute(Variable variable) {
		return second.getSubstitute(first.getSubstitute(variable));
	}
}
