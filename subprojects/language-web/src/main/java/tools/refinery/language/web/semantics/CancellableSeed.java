/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import tools.refinery.store.map.AnyVersionedMap;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.viatra.runtime.CancellationToken;

import java.util.Set;

class CancellableSeed<T> implements Seed<T> {
	private final CancellationToken cancellationToken;
	private final Seed<T> seed;

	private CancellableSeed(CancellationToken cancellationToken, Seed<T> seed) {
		this.cancellationToken = cancellationToken;
		this.seed = seed;
	}

	@Override
	public int arity() {
		return seed.arity();
	}

	@Override
	public Class<T> valueType() {
		return seed.valueType();
	}

	@Override
	public T reducedValue() {
		return seed.reducedValue();
	}

	@Override
	public T get(Tuple key) {
		return seed.get(key);
	}

	@Override
	public Cursor<Tuple, T> getCursor(T defaultValue, int nodeCount) {
		return new CancellableCursor<>(cancellationToken, seed.getCursor(defaultValue, nodeCount));
	}

	public static ModelSeed wrap(CancellationToken cancellationToken, ModelSeed modelSeed) {
		var builder = ModelSeed.builder(modelSeed.getNodeCount());
		for (var partialSymbol : modelSeed.getSeededSymbols()) {
			wrap(cancellationToken, (PartialSymbol<?, ?>) partialSymbol, modelSeed, builder);
		}
		return builder.build();
	}

	private static <A, C> void wrap(CancellationToken cancellationToken, PartialSymbol<A, C> partialSymbol,
									ModelSeed originalModelSeed, ModelSeed.Builder builder) {
		var originalSeed = originalModelSeed.getSeed(partialSymbol);
		builder.seed(partialSymbol, new CancellableSeed<>(cancellationToken, originalSeed));
	}

	private record CancellableCursor<T>(CancellationToken cancellationToken, Cursor<Tuple, T> cursor)
			implements Cursor<Tuple, T> {
		@Override
		public Tuple getKey() {
			return cursor.getKey();
		}

		@Override
		public T getValue() {
			return cursor.getValue();
		}

		@Override
		public boolean isTerminated() {
			return cursor.isTerminated();
		}

		@Override
		public boolean move() {
			cancellationToken.checkCancelled();
			return cursor.move();
		}

		@Override
		public boolean isDirty() {
			return cursor.isDirty();
		}

		@Override
		public Set<AnyVersionedMap> getDependingMaps() {
			return cursor.getDependingMaps();
		}
	}
}
