/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import org.jetbrains.annotations.Nullable;

public sealed interface Sort permits DataSort, NodeSort {
	boolean isInstance(Variable variable);

	Variable newInstance(@Nullable String name);

	Variable newInstance();
}
