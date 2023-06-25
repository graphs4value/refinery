/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.equality;

import tools.refinery.store.query.term.Variable;

import java.util.LinkedHashMap;
import java.util.Map;

public class SubstitutingLiteralHashCodeHelper implements LiteralHashCodeHelper {
	private final Map<Variable, Integer> assignedHashCodes = new LinkedHashMap<>();

	// 0 is for {@code null}, so we start with 1.
	private int next = 1;

	@Override
	public int getVariableHashCode(Variable variable) {
		if (variable == null) {
			return 0;
		}
		return assignedHashCodes.computeIfAbsent(variable, key -> {
			int sequenceNumber = next;
			next++;
			return variable.hashCodeWithSubstitution(sequenceNumber);
		});
	}
}
