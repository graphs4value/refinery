/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.ParametricDefinition;

public interface WrappedParametricDefinition {
	ParametricDefinition get();

	default Parameter param(int i) {
		return get().getParameters().get(i);
	}
}
