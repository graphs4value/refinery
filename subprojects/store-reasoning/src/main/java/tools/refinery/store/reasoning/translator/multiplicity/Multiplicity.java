/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiplicity;

import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;

public sealed interface Multiplicity permits ConstrainedMultiplicity, UnconstrainedMultiplicity {
	CardinalityInterval multiplicity();

	boolean isConstrained();
}
