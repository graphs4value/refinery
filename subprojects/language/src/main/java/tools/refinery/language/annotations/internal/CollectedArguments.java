/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import tools.refinery.language.model.problem.AnnotationArgument;

import java.util.List;

record CollectedArguments(List<AnnotationArgument> arguments, boolean optional, boolean repeatable) {
}
