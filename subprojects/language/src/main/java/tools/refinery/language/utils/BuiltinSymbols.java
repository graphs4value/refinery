/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import tools.refinery.language.model.problem.*;

public record BuiltinSymbols(Problem problem, ClassDeclaration node, ReferenceDeclaration equals,
							 PredicateDefinition exists, PredicateDefinition contained, PredicateDefinition contains,
							 PredicateDefinition root) {
}
