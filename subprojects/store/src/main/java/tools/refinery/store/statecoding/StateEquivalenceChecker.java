/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.model.AnyInterpretation;

import java.util.List;

public interface StateEquivalenceChecker {
	enum EquivalenceResult {
		ISOMORPHIC, UNKNOWN, DIFFERENT
	}

	EquivalenceResult constructMorphism(
			IntSet individuals, List<? extends AnyInterpretation> interpretations1, ObjectCode code1,
			List<? extends AnyInterpretation> interpretations2, ObjectCode code2);
}
