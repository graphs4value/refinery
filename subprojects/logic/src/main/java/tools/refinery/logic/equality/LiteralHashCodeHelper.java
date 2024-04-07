/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.equality;

import tools.refinery.logic.term.Variable;

import java.util.Objects;

@FunctionalInterface
public interface LiteralHashCodeHelper {
	LiteralHashCodeHelper DEFAULT = Objects::hashCode;

	int getVariableHashCode(Variable variable);
}
