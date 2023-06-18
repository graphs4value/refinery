/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import tools.refinery.store.reasoning.representation.AnyPartialSymbol;

public sealed interface AnyPartialInterpretation permits PartialInterpretation {
	ReasoningAdapter getAdapter();

	AnyPartialSymbol getPartialSymbol();

	int countUnfinished();

	int countErrors();
}
