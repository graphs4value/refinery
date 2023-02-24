package tools.refinery.store.query.tests;

import org.hamcrest.Description;
import tools.refinery.store.query.equality.DeepDnfEqualityChecker;

class MismatchDescribingDnfEqualityChecker extends DeepDnfEqualityChecker {
	private final Description description;
	private boolean described;

	MismatchDescribingDnfEqualityChecker(Description description) {
		this.description = description;
	}

	public boolean isDescribed() {
		return described;
	}

	@Override
	protected boolean doCheckEqual(Pair pair) {
		boolean result = super.doCheckEqual(pair);
		if (!result && !described) {
			describeMismatch(pair);
			// Only describe the first found (innermost) mismatch.
			described = true;
		}
		return result;
	}

	private void describeMismatch(Pair pair) {
		var inProgress = getInProgress();
		int size = inProgress.size();
		if (size <= 1) {
			description.appendText("was ").appendValue(pair.left());
			return;
		}
		var last = inProgress.get(size - 1);
		description.appendText("expected ").appendValue(last.right());
		for (int i = size - 2; i >= 0; i--) {
			description.appendText(" called from ").appendText(inProgress.get(i).left().name());
		}
		description.appendText(" was not structurally equal to ").appendValue(last.right());
	}
}
