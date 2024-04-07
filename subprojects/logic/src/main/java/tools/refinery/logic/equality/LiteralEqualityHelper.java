/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.equality;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.term.Variable;

import java.util.Objects;

public interface LiteralEqualityHelper extends DnfEqualityChecker {
	LiteralEqualityHelper DEFAULT = new LiteralEqualityHelper() {
		@Override
		public boolean variableEqual(Variable left, Variable right) {
			return Objects.equals(left, right);
		}

		@Override
		public boolean dnfEqual(Dnf left, Dnf right) {
			return DnfEqualityChecker.DEFAULT.dnfEqual(left, right);
		}
	};

	boolean variableEqual(Variable left, Variable right);
}
