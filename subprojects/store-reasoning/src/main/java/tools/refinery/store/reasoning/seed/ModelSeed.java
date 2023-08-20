/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.seed;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ModelSeed {
	private final int nodeCount;
	private final Map<AnyPartialSymbol, Seed<?>> seeds;

	private ModelSeed(int nodeCount, Map<AnyPartialSymbol, Seed<?>> seeds) {
		this.nodeCount = nodeCount;
		this.seeds = seeds;
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public <A> Seed<A> getSeed(PartialSymbol<A, ?> partialSymbol) {
		var seed = seeds.get(partialSymbol);
		if (seed == null) {
			throw new IllegalArgumentException("No seed for partial symbol " + partialSymbol);
		}
		// The builder makes sure only well-typed seeds can be added.
		@SuppressWarnings("unchecked")
		var typedSeed = (Seed<A>) seed;
		return typedSeed;
	}

	public boolean containsSeed(AnyPartialSymbol symbol) {
		return seeds.containsKey(symbol);
	}

	public Set<AnyPartialSymbol> getSeededSymbols() {
		return Collections.unmodifiableSet(seeds.keySet());
	}

	public <A> Cursor<Tuple, A> getCursor(PartialSymbol<A, ?> partialSymbol, A defaultValue) {
		return getSeed(partialSymbol).getCursor(defaultValue, nodeCount);
	}

	public static Builder builder(int nodeCount) {
		return new Builder(nodeCount);
	}

	public static class Builder {
		private final int nodeCount;
		private final Map<AnyPartialSymbol, Seed<?>> seeds = new LinkedHashMap<>();

		private Builder(int nodeCount) {
			if (nodeCount < 0) {
				throw new IllegalArgumentException("Node count must not be negative");
			}
			this.nodeCount = nodeCount;
		}

		public <A> Builder seed(PartialSymbol<A, ?> partialSymbol, Seed<A> seed) {
			if (seed.arity() != partialSymbol.arity()) {
				throw new IllegalStateException("Expected seed of arity %d for partial symbol %s, but got %d instead"
						.formatted(partialSymbol.arity(), partialSymbol, seed.arity()));
			}
			if (!seed.valueType().equals(partialSymbol.abstractDomain().abstractType())) {
				throw new IllegalStateException("Expected seed of type %s for partial symbol %s, but got %s instead"
						.formatted(partialSymbol.abstractDomain().abstractType(), partialSymbol, seed.valueType()));
			}
			if (seeds.put(partialSymbol, seed) != null) {
				throw new IllegalArgumentException("Duplicate seed for partial symbol " + partialSymbol);
			}
			return this;
		}

		public <A> Builder seed(PartialSymbol<A, ?> partialSymbol, Consumer<Seed.Builder<A>> callback) {
			var builder = Seed.builder(partialSymbol);
			callback.accept(builder);
			return seed(partialSymbol, builder.build());
		}

		public ModelSeed build() {
			return new ModelSeed(nodeCount, Collections.unmodifiableMap(seeds));
		}
	}
}
