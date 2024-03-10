/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.cardinalityinterval;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;

public sealed interface CardinalityInterval extends AbstractValue<CardinalityInterval, Integer>
		permits NonEmptyCardinalityInterval, EmptyCardinalityInterval {
	int lowerBound();

	UpperCardinality upperBound();

	CardinalityInterval min(CardinalityInterval other);

	CardinalityInterval max(CardinalityInterval other);

	CardinalityInterval add(CardinalityInterval other);

	CardinalityInterval take(int count);

	CardinalityInterval multiply(CardinalityInterval other);
}
