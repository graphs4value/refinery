/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.representation.AnySymbol;

public sealed interface AnyInterpretation permits Interpretation {
	Model getModel();

	AnySymbol getSymbol();

	long getSize();

	int getAdjacentSize(int slot, int node);
}
