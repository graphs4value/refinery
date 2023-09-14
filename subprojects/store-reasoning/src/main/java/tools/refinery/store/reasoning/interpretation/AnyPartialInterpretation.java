/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;

public sealed interface AnyPartialInterpretation permits PartialInterpretation {
	ReasoningAdapter getAdapter();

	AnyPartialSymbol getPartialSymbol();

	Concreteness getConcreteness();
}
