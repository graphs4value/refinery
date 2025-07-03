/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.truthvalue.TruthValue;

public interface AbstractValue<A extends AbstractValue<A, C>, C> {
	@Nullable
	C getConcrete();

	default boolean isConcrete() {
		return getConcrete() == null;
	}

	@Nullable
	C getArbitrary();

	default boolean isError() {
		return getArbitrary() == null;
	}

	A join(A other);

	A meet(A other);

	default boolean isRefinementOf(A other) {
		return equals(meet(other));
	}

	default boolean isOverlapping(A other) {
		return !meet(other).isError();
	}

	default TruthValue checkEquals(A other) {
		if (isError() || other.isError()) {
			return TruthValue.ERROR;
		}
		if (!isOverlapping(other)) {
			return TruthValue.FALSE;
		}
		return isConcrete() && other.isConcrete() ? TruthValue.TRUE : TruthValue.UNKNOWN;
	}
}
