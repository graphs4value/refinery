/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.tests;

import org.hamcrest.Description;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.SymbolicParameter;
import tools.refinery.store.query.equality.DeepDnfEqualityChecker;
import tools.refinery.store.query.literal.Literal;

import java.util.List;

class MismatchDescribingDnfEqualityChecker extends DeepDnfEqualityChecker {
	private final Description description;
	private boolean raw;
	private boolean needsDescription = true;

	MismatchDescribingDnfEqualityChecker(Description description) {
		this.description = description;
	}

	public boolean needsDescription() {
		return needsDescription;
	}

	@Override
	public boolean dnfEqualRaw(List<SymbolicParameter> symbolicParameters, List<? extends List<? extends Literal>> clauses, Dnf other) {
		try {
			raw = true;
			boolean result = super.dnfEqualRaw(symbolicParameters, clauses, other);
			if (!result && needsDescription) {
				description.appendText("was ").appendText(other.toDefinitionString());
			}
			return false;
		} finally {
			raw = false;
		}
	}

	@Override
	protected boolean doCheckEqual(Pair pair) {
		boolean result = super.doCheckEqual(pair);
		if (!result && needsDescription) {
			describeMismatch(pair);
			// Only describe the first found (innermost) mismatch.
			needsDescription = false;
		}
		return result;
	}

	private void describeMismatch(Pair pair) {
		var inProgress = getInProgress();
		int size = inProgress.size();
		if (size <= 1 && !raw) {
			description.appendText("was ").appendText(pair.right().toDefinitionString());
			return;
		}
		var last = inProgress.get(size - 1);
		description.appendText("expected ").appendText(last.left().toDefinitionString());
		for (int i = size - 2; i >= 0; i--) {
			description.appendText(" called from ").appendText(inProgress.get(i).left().toString());
		}
		description.appendText(" was not structurally equal to ").appendText(last.right().toDefinitionString());
	}
}
