/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.Concreteness;
import tools.refinery.language.model.problem.Modality;
import tools.refinery.logic.Constraint;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.ModalitySpecification;

record ConcreteModality(ConcretenessSpecification concreteness, ModalitySpecification modality) {
	public static final ConcreteModality NULL =
			new ConcreteModality(ConcretenessSpecification.UNSPECIFIED, ModalitySpecification.UNSPECIFIED);
	public static final ConcreteModality PARTIAL_MAY =
			new ConcreteModality(ConcretenessSpecification.PARTIAL, ModalitySpecification.MAY);
	public static final ConcreteModality CANDIDATE_MUST =
			new ConcreteModality(ConcretenessSpecification.CANDIDATE, ModalitySpecification.MUST);

	public ConcreteModality(Concreteness concreteness, Modality modality) {
		this(
				switch (concreteness) {
					case UNSPECIFIED -> ConcretenessSpecification.UNSPECIFIED;
					case PARTIAL -> ConcretenessSpecification.PARTIAL;
					case CANDIDATE -> ConcretenessSpecification.CANDIDATE;
				},
				switch (modality) {
					case UNSPECIFIED -> ModalitySpecification.UNSPECIFIED;
					case MUST -> ModalitySpecification.MUST;
					case MAY -> ModalitySpecification.MAY;
				}
		);
	}

	public ConcreteModality negate() {
		var negatedModality = modality.negate();
		return new ConcreteModality(concreteness, negatedModality);
	}

	public ConcreteModality merge(ConcreteModality outer) {
		var mergedConcreteness = concreteness.orElse(outer.concreteness);
		var mergedModality = modality.orElse(outer.modality);
		return new ConcreteModality(mergedConcreteness, mergedModality);
	}

	public Constraint wrapConstraint(Constraint inner) {
		if (isSet()) {
			return new ModalConstraint(modality, concreteness, inner);
		}
		return inner;
	}

	public boolean isSet() {
		return concreteness != ConcretenessSpecification.UNSPECIFIED ||
				modality != ModalitySpecification.UNSPECIFIED;
	}
}
