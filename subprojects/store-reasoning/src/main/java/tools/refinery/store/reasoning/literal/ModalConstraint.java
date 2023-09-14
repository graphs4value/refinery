/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.literal.Reduction;
import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.query.view.AnySymbolView;

import java.util.List;

public record ModalConstraint(Modality modality, Concreteness concreteness, Constraint constraint)
		implements Constraint {
	public ModalConstraint {
		if (constraint instanceof AnySymbolView || constraint instanceof ModalConstraint) {
			throw new InvalidQueryException("Already concrete constraints cannot be abstracted");
		}
	}

	public ModalConstraint(Modality modality, Constraint constraint) {
		this(modality, Concreteness.PARTIAL, constraint);
	}

	@Override
	public String name() {
		return formatName(constraint.name());
	}

	@Override
	public List<Parameter> getParameters() {
		return constraint.getParameters();
	}

	@Override
	public Reduction getReduction() {
		return constraint.getReduction();
	}

	@Override
	public boolean equals(LiteralEqualityHelper helper, Constraint other) {
		if (getClass() != other.getClass()) {
			return false;
		}
		var otherModalConstraint = (ModalConstraint) other;
		return modality == otherModalConstraint.modality &&
				concreteness == otherModalConstraint.concreteness &&
				constraint.equals(helper, otherModalConstraint.constraint);
	}

	@Override
	public String toReferenceString() {
		return formatName(constraint.toReferenceString());
	}

	@Override
	public String toString() {
		return formatName(constraint.toString());
	}

	private String formatName(String constraintName) {
		if (concreteness == Concreteness.PARTIAL) {
			return "%s %s".formatted(modality, constraintName);
		}
		return "%s %s %s".formatted(modality, concreteness, constraintName);
	}

	public static Constraint of(Modality modality, Concreteness concreteness, Constraint constraint) {
		if (constraint instanceof AnySymbolView || constraint instanceof ModalConstraint) {
			// Symbol views and lifted constraints are already concrete. Thus, they cannot be abstracted at all.
			return constraint;
		}
		return new ModalConstraint(modality, concreteness, constraint);
	}
}
