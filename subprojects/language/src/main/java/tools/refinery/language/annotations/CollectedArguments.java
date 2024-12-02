/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import tools.refinery.language.model.problem.Expr;

import java.util.List;

record CollectedArguments(List<Expr> values, boolean optional, boolean repeatable) {
}
