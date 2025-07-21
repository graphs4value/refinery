/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.literal.Reduction;
import tools.refinery.logic.term.Parameter;
import tools.refinery.store.query.view.AnySymbolView;

import java.util.List;

public record ModalConstraint(ModalitySpecification modality, ConcretenessSpecification concreteness,
							  Constraint constraint)
		implements Constraint {
	public ModalConstraint {
		if (constraint instanceof AnySymbolView || constraint instanceof ModalConstraint) {
			throw new InvalidQueryException("Already concrete constraints cannot be abstracted");
		}
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
	public @NotNull String toString() {
		return formatName(constraint.toString());
	}

	private String formatName(String constraintName) {
		if (concreteness == ConcretenessSpecification.PARTIAL) {
			return "%s %s".formatted(modality, constraintName);
		}
		return "%s %s %s".formatted(modality, concreteness, constraintName);
	}

	public static Constraint of(Modality modality, Concreteness concreteness, Constraint constraint) {
		return of(modality.toSpecification(), concreteness.toSpecification(), constraint);
	}

	public static Constraint of(Modality modality, Constraint constraint) {
		return of(modality.toSpecification(), ConcretenessSpecification.UNSPECIFIED, constraint);
	}

	public static Constraint of(Concreteness concreteness, Constraint constraint) {
		return of(ModalitySpecification.UNSPECIFIED, concreteness.toSpecification(), constraint);
	}

	public static Constraint of(ModalitySpecification modality, ConcretenessSpecification concreteness,
								Constraint constraint) {
		return switch (constraint) {
			case AnySymbolView anySymbolView -> anySymbolView;
			case ModalConstraint(var otherModality, var otherConcreteness, var innerConstraint) ->
					new ModalConstraint(otherModality.orElse(modality), otherConcreteness.orElse(concreteness),
							innerConstraint);
			default -> new ModalConstraint(modality, concreteness, constraint);
		};
	}
}
