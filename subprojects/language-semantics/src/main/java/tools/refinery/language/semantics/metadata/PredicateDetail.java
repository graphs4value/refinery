/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.metadata;

import java.util.List;

public record PredicateDetail(PredicateDetailKind predicateKind, List<String> parameterNames)
		implements RelationDetail {
}
