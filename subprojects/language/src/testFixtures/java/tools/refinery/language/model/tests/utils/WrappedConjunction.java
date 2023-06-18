/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Conjunction;

public record WrappedConjunction(Conjunction conjunction) {
	public Conjunction get() {
		return conjunction;
	}
	
	public WrappedLiteral lit(int i) {
		return new WrappedLiteral(conjunction.getLiterals().get(i));
	}
}
