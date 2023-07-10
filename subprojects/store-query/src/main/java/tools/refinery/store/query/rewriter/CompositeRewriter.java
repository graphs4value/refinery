/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.rewriter;

import tools.refinery.store.query.dnf.Dnf;

import java.util.ArrayList;
import java.util.List;

public class CompositeRewriter implements DnfRewriter {
	private final List<DnfRewriter> rewriterList = new ArrayList<>();

	public void addFirst(DnfRewriter rewriter) {
		rewriterList.add(rewriter);
	}

	@Override
	public Dnf rewrite(Dnf dnf) {
		Dnf rewrittenDnf = dnf;
		for (int i = rewriterList.size() - 1; i >= 0; i--) {
			var rewriter = rewriterList.get(i);
			rewrittenDnf = rewriter.rewrite(rewrittenDnf);
		}
		return rewrittenDnf;
	}
}
