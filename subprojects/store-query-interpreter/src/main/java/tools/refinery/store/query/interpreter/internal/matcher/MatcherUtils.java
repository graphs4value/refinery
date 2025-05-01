/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.tuple.*;

import java.util.Iterator;

final class MatcherUtils {
	private MatcherUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static tools.refinery.interpreter.matchers.tuple.Tuple toInterpreterTuple(Tuple refineryTuple) {
		return switch (refineryTuple) {
			case Tuple0 ignored -> Tuples.staticArityFlatTupleOf();
			case Tuple1 tuple1 -> Tuples.staticArityFlatTupleOf(tuple1);
			case Tuple2(int value0, int value1) -> Tuples.staticArityFlatTupleOf(Tuple.of(value0), Tuple.of(value1));
			case Tuple3(int value0, int value1, int value2) ->
					Tuples.staticArityFlatTupleOf(Tuple.of(value0), Tuple.of(value1), Tuple.of(value2));
			case Tuple4(int value0, int value1, int value2, int value3) ->
					Tuples.staticArityFlatTupleOf(Tuple.of(value0), Tuple.of(value1), Tuple.of(value2),
							Tuple.of(value3));
			default -> {
				int arity = refineryTuple.getSize();
				var values = new Object[arity];
				for (int i = 0; i < arity; i++) {
					values[i] = Tuple.of(refineryTuple.get(i));
				}
				yield Tuples.flatTupleOf(values);
			}
		};
	}

	public static Tuple toRefineryTuple(ITuple interpreterTuple) {
		int arity = interpreterTuple.getSize();
		if (arity == 1) {
			return getWrapper(interpreterTuple, 0);
		}
		return prefixToRefineryTuple(interpreterTuple, interpreterTuple.getSize());
	}

	public static Tuple keyToRefineryTuple(ITuple interpreterTuple) {
		return prefixToRefineryTuple(interpreterTuple, interpreterTuple.getSize() - 1);
	}

	private static Tuple prefixToRefineryTuple(ITuple interpreterTuple, int targetArity) {
		if (targetArity < 0) {
			throw new IllegalArgumentException("Requested negative prefix %d of %s"
					.formatted(targetArity, interpreterTuple));
		}
		return switch (targetArity) {
			case 0 -> Tuple.of();
			case 1 -> Tuple.of(unwrap(interpreterTuple, 0));
			case 2 -> Tuple.of(unwrap(interpreterTuple, 0), unwrap(interpreterTuple, 1));
			case 3 -> Tuple.of(unwrap(interpreterTuple, 0), unwrap(interpreterTuple, 1), unwrap(interpreterTuple, 2));
			case 4 -> Tuple.of(unwrap(interpreterTuple, 0), unwrap(interpreterTuple, 1), unwrap(interpreterTuple, 2),
					unwrap(interpreterTuple, 3));
			default -> {
				var entries = new int[targetArity];
				for (int i = 0; i < targetArity; i++) {
					entries[i] = unwrap(interpreterTuple, i);
				}
				yield Tuple.of(entries);
			}
		};
	}

	private static Tuple1 getWrapper(ITuple interpreterTuple, int index) {
		if (!((interpreterTuple.get(index)) instanceof Tuple1 wrappedObjectId)) {
			throw new IllegalArgumentException("Element %d of tuple %s is not an object id"
					.formatted(index, interpreterTuple));
		}
		return wrappedObjectId;
	}

	private static int unwrap(ITuple interpreterTuple, int index) {
		return getWrapper(interpreterTuple, index).value0();
	}

	public static <T> T getValue(ITuple match) {
		// This is only safe if we know for sure that match came from a functional query of type {@code T}.
		@SuppressWarnings("unchecked")
		var result = (T) match.get(match.getSize() - 1);
		return result;
	}

	public static <T> T getSingleValue(@Nullable Iterable<? extends ITuple> interpreterTuples) {
		if (interpreterTuples == null) {
			return null;
		}
		return getSingleValue(interpreterTuples.iterator());
	}

	public static <T> T getSingleValue(Iterator<? extends ITuple> iterator) {
		if (!iterator.hasNext()) {
			return null;
		}
		var match = iterator.next();
		var result = MatcherUtils.<T>getValue(match);
		if (iterator.hasNext()) {
			var input = keyToRefineryTuple(match);
			throw new IllegalStateException("Query is not functional for input tuple: " + input);
		}
		return result;
	}
}
