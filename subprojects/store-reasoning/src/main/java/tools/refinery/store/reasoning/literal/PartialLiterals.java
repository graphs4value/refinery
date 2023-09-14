/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.literal.CallLiteral;

public final class PartialLiterals {
	private PartialLiterals() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static CallLiteral may(CallLiteral literal) {
		return addModality(literal, Modality.MAY, Concreteness.PARTIAL);
	}

	public static CallLiteral must(CallLiteral literal) {
		return addModality(literal, Modality.MUST, Concreteness.PARTIAL);
	}

	public static CallLiteral candidateMay(CallLiteral literal) {
		return addModality(literal, Modality.MAY, Concreteness.CANDIDATE);
	}

	public static CallLiteral candidateMust(CallLiteral literal) {
		return addModality(literal, Modality.MUST, Concreteness.CANDIDATE);
	}

	public static CallLiteral addModality(CallLiteral literal, Modality modality, Concreteness concreteness) {
		var target = literal.getTarget();
		if (target instanceof ModalConstraint) {
			throw new InvalidQueryException("Literal %s already has modality".formatted(literal));
		}
		var polarity = literal.getPolarity();
		var modalTarget = new ModalConstraint(modality.commute(polarity), concreteness, target);
		return new CallLiteral(polarity, modalTarget, literal.getArguments());
	}
}
