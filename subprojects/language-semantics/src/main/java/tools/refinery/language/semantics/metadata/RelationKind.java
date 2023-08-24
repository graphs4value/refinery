/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.metadata;

public enum RelationKind {
	BUILTIN,
	CLASS,
	ENUM,
	REFERENCE,
	OPPOSITE,
	CONTAINMENT,
	CONTAINER,
	PREDICATE,
	ERROR
}
