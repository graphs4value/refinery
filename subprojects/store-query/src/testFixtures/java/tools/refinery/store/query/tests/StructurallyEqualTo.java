package tools.refinery.store.query.tests;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.equality.DeepDnfEqualityChecker;

public class StructurallyEqualTo extends TypeSafeMatcher<Dnf> {
	private final Dnf expected;

	public StructurallyEqualTo(Dnf expected) {
		this.expected = expected;
	}

	@Override
	protected boolean matchesSafely(Dnf item) {
		var checker = new DeepDnfEqualityChecker();
		return checker.dnfEqual(expected, item);
	}

	@Override
	protected void describeMismatchSafely(Dnf item, Description mismatchDescription) {
		var describingChecker = new MismatchDescribingDnfEqualityChecker(mismatchDescription);
		if (describingChecker.dnfEqual(expected, item)) {
			throw new IllegalStateException("Mismatched Dnf was matching on repeated comparison");
		}
		if (!describingChecker.isDescribed()) {
			super.describeMismatchSafely(item, mismatchDescription);
		}
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("structurally equal to ").appendValue(expected);
	}
}
