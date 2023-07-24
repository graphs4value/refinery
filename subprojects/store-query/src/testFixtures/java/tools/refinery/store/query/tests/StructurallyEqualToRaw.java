/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.tests;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.SymbolicParameter;
import tools.refinery.store.query.equality.DeepDnfEqualityChecker;
import tools.refinery.store.query.literal.Literal;

import java.util.List;

public class StructurallyEqualToRaw extends TypeSafeMatcher<Dnf> {
	private final List<SymbolicParameter> expectedSymbolicParameters;
	private final List<? extends List<? extends Literal>> expectedClauses;

	public StructurallyEqualToRaw(List<SymbolicParameter> expectedSymbolicParameters,
								  List<? extends List<? extends Literal>> expectedClauses) {
		this.expectedSymbolicParameters = expectedSymbolicParameters;
		this.expectedClauses = expectedClauses;
	}

	@Override
	protected boolean matchesSafely(Dnf item) {
		var checker = new DeepDnfEqualityChecker();
		return checker.dnfEqualRaw(expectedSymbolicParameters, expectedClauses, item);
	}

	@Override
	protected void describeMismatchSafely(Dnf item, Description mismatchDescription) {
		var describingChecker = new MismatchDescribingDnfEqualityChecker(mismatchDescription);
		if (describingChecker.dnfEqualRaw(expectedSymbolicParameters, expectedClauses, item)) {
			throw new IllegalStateException("Mismatched Dnf was matching on repeated comparison");
		}
		if (describingChecker.needsDescription()) {
			super.describeMismatchSafely(item, mismatchDescription);
		}
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("structurally equal to ")
				.appendValueList("(", ", ", ")", expectedSymbolicParameters)
				.appendText(" <-> ")
				.appendValueList("", ", ", ".", expectedClauses);
	}
}
