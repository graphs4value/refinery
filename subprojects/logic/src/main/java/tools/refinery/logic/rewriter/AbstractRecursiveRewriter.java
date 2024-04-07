/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.rewriter;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.equality.DnfEqualityChecker;
import tools.refinery.logic.util.CycleDetectingMapper;

public abstract class AbstractRecursiveRewriter implements DnfRewriter {
	private final CycleDetectingMapper<Dnf, Dnf> mapper = new CycleDetectingMapper<>(Dnf::name, this::map);

	@Override
	public Dnf rewrite(Dnf dnf) {
		return mapper.map(dnf);
	}

	protected Dnf map(Dnf dnf) {
		var result = doRewrite(dnf);
		return dnf.equalsWithSubstitution(DnfEqualityChecker.DEFAULT, result) ? dnf : result;
	}

	protected abstract Dnf doRewrite(Dnf dnf);
}
