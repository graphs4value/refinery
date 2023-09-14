/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;

public sealed interface AnyPartialInterpretationRefiner permits PartialInterpretationRefiner {
	ReasoningAdapter getAdapter();

	AnyPartialSymbol getPartialSymbol();
}
