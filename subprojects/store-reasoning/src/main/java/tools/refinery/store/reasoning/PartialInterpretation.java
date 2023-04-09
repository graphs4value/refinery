/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

public non-sealed interface PartialInterpretation<A, C> extends AnyPartialInterpretation {
	@Override
	PartialSymbol<A, C> getPartialSymbol();

	A get(Tuple key);

	Cursor<Tuple, A> getAll();

	MergeResult merge(Tuple key, A value);

	C getConcrete(Tuple key);

	Cursor<Tuple, C> getAllConcrete();
}
