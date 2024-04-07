/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.tests;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.term.Parameter;

import java.util.Arrays;
import java.util.List;

public record FakeKeyOnlyView(String name, int arity) implements Constraint {
	@Override
	public List<Parameter> getParameters() {
		var parameters = new Parameter[arity];
		Arrays.fill(parameters, Parameter.NODE_OUT);
		return List.of(parameters);
	}
}
