/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.literal.LiteralReduction;
import tools.refinery.store.query.term.Parameter;

import java.util.List;

public record ModalConstraint(Modality modality, Constraint constraint) implements Constraint {
	private static final String FORMAT = "%s %s";

	@Override
	public String name() {
		return FORMAT.formatted(modality, constraint.name());
	}

	@Override
	public List<Parameter> getParameters() {
		return constraint.getParameters();
	}

	@Override
	public LiteralReduction getReduction() {
		return constraint.getReduction();
	}

	@Override
	public boolean equals(LiteralEqualityHelper helper, Constraint other) {
		if (getClass() != other.getClass()) {
			return false;
		}
		var otherModalConstraint = (ModalConstraint) other;
		return modality == otherModalConstraint.modality && constraint.equals(helper, otherModalConstraint.constraint);
	}

	@Override
	public String toReferenceString() {
		return FORMAT.formatted(modality, constraint.toReferenceString());
	}

	@Override
	public String toString() {
		return FORMAT.formatted(modality, constraint);
	}
}
