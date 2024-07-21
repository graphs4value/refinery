/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.utils;

import tools.refinery.language.model.problem.Action;

public record WrappedAction(Action action) {
	public Action get() {
		return action;
	}
}
