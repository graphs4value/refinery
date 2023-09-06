/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.tuple.Tuple;

@FunctionalInterface
public interface BoundActionLiteral {
	@Nullable
	Tuple fire(@NotNull Tuple tuple);
}
