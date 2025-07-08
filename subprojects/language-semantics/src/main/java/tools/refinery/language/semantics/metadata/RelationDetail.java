/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.metadata;

import org.jetbrains.annotations.Nullable;

public sealed interface RelationDetail {
	record Class(boolean isAbstract, @Nullable String color) implements RelationDetail {
	}

	record Computed(String of) implements RelationDetail {
	}

	record Opposite(String of, boolean isContainer) implements RelationDetail {
	}

	record Predicate(PredicateDetailKind kind) implements RelationDetail {
	}

	record Reference(boolean isContainment) implements RelationDetail {
	}

	record Attribute() implements RelationDetail {
	}
}
