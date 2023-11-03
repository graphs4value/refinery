/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics.metadata;

public record PredicateDetail(boolean error) implements RelationDetail {
	public static final PredicateDetail PREDICATE = new PredicateDetail(false);

	public static final PredicateDetail ERROR_PREDICATE = new PredicateDetail(true);

	public static PredicateDetail ofError(boolean error) {
		return error ? ERROR_PREDICATE : PREDICATE;
	}
}
