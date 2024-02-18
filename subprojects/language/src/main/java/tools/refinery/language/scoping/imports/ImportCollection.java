/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import org.eclipse.emf.common.util.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ImportCollection {
	public static ImportCollection EMPTY = new ImportCollection() {
		@Override
		public void add(Import importEntry) {
			throw new UnsupportedOperationException("Read-only collection");
		}

		@Override
		public void remove(URI uri) {
			throw new UnsupportedOperationException("Read-only collection");
		}
	};

	private final Map<URI, Import> importMap = new HashMap<>();

	public void add(Import importEntry) {
		importMap.compute(importEntry.uri(), (ignored, originalEntry) -> merge(originalEntry, importEntry));
	}

	public void addAll(Iterable<? extends Import> imports) {
		imports.forEach(this::add);
	}

	public void remove(URI uri) {
		importMap.remove(uri);
	}

	public List<Import> toList() {
		return List.copyOf(importMap.values());
	}

	public Set<URI> toUriSet() {
		return new LinkedHashSet<>(importMap.keySet());
	}

	@NotNull
	private static Import merge(@Nullable Import left, @NotNull Import right) {
		if (left == null) {
			return right;
		}
		if (!left.uri().equals(right.uri())) {
			throw new IllegalArgumentException("Expected URIs '%s' and '%s' to be equal".formatted(
					left.uri(), right.uri()));
		}
		return switch (left) {
			case TransitiveImport transitiveLeft ->
					right instanceof TransitiveImport ? left : merge(right, transitiveLeft);
			case NamedImport namedLeft -> switch (right) {
				case TransitiveImport ignored -> namedLeft;
				case NamedImport namedRight -> {
					if (!namedLeft.qualifiedName().equals(namedRight.qualifiedName())) {
						throw new IllegalArgumentException("Expected qualified names '%s' and '%s' to be equal"
								.formatted(namedLeft.qualifiedName(), namedRight.qualifiedName()));
					}
					var mergedAliases = new LinkedHashSet<>(namedLeft.aliases());
					mergedAliases.addAll(namedRight.aliases());
					yield new NamedImport(namedLeft.uri(), namedLeft.qualifiedName(),
							List.copyOf(mergedAliases), namedLeft.alsoImplicit() || namedRight.alsoImplicit());
				}
			};
		};
	}
}
