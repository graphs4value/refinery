/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import tools.refinery.store.model.AnyInterpretation;
import tools.refinery.store.statecoding.neighborhood.IndividualsSet;

import java.util.List;

public interface StateEquivalenceChecker {
	enum EquivalenceResult {
		ISOMORPHIC, UNKNOWN, DIFFERENT
	}

	EquivalenceResult constructMorphism(
			IndividualsSet individuals, List<? extends AnyInterpretation> interpretations1, ObjectCode code1,
			List<? extends AnyInterpretation> interpretations2, ObjectCode code2);
}
