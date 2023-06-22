/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.query.view.TuplePreservingView;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

class InferredMayTypeView extends TuplePreservingView<InferredType> {
	private final PartialRelation type;

	InferredMayTypeView(PartialRelation type) {
		super(TypeHierarchyTranslationUnit.INFERRED_TYPE_SYMBOL, "%s#may".formatted(type));
		this.type = type;
	}

	@Override
	protected boolean doFilter(Tuple key, InferredType value) {
		return value.mayConcreteTypes().contains(type);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		InferredMayTypeView that = (InferredMayTypeView) o;
		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type);
	}
}
