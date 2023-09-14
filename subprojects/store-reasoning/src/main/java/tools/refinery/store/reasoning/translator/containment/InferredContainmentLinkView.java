/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.query.view.TuplePreservingView;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.Symbol;

import java.util.Objects;

abstract class InferredContainmentLinkView extends TuplePreservingView<InferredContainment> {
	protected final PartialRelation link;

	protected InferredContainmentLinkView(Symbol<InferredContainment> symbol, String name, PartialRelation link) {
		super(symbol, link.name() + "#" + name);
		this.link = link;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		InferredContainmentLinkView that = (InferredContainmentLinkView) o;
		return Objects.equals(link, that.link);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), link);
	}
}
