/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.literal.Reduction;
import tools.refinery.logic.term.Parameter;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.List;

public record ComputedConstraint(PartialRelation partialRelation) implements Constraint {
	@Override
	public String name() {
		return formatName(partialRelation.name());
	}

	@Override
	public List<Parameter> getParameters() {
		return partialRelation.getParameters();
	}

	@Override
	public Reduction getReduction() {
		// Never reducible, even if the underlying constraint is reducible,
		// because the computation may return a value different from the prescribed value.
		return Reduction.NOT_REDUCIBLE;
	}

	@Override
	public boolean equals(LiteralEqualityHelper helper, Constraint other) {
		if (getClass() != other.getClass()) {
			return false;
		}
		var otherComputedConstraint = (ComputedConstraint) other;
		return partialRelation.equals(helper, otherComputedConstraint.partialRelation);
	}

	@Override
	public String toReferenceString() {
		return formatName(partialRelation.toReferenceString());
	}

	@Override
	public String toString() {
		return formatName(partialRelation.toString());
	}

	private String formatName(String constraintName) {
		return "@Computed " + constraintName;
	}
}
