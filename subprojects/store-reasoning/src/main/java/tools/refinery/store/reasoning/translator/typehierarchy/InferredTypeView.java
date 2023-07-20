/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.query.view.TuplePreservingView;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.Symbol;

import java.util.Objects;

abstract class InferredTypeView extends TuplePreservingView<InferredType> {
	protected final PartialRelation type;

	protected InferredTypeView(Symbol<InferredType> symbol, String name, PartialRelation type) {
		super(symbol, type.name() + "#" + name);
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		InferredTypeView that = (InferredTypeView) o;
		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type);
	}
}
