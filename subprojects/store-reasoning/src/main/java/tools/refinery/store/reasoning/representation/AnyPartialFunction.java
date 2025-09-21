/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;

import java.util.List;

public sealed interface AnyPartialFunction extends AnyPartialSymbol permits PartialFunction {
	AnyTerm call(NodeVariable... arguments);

	AnyTerm call(Concreteness concreteness, NodeVariable... arguments);

	AnyTerm call(ConcretenessSpecification concreteness, List<NodeVariable> arguments);
}
