/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import org.eclipse.xtext.web.server.validation.ValidationResult;

import java.util.List;

public record SemanticsIssuesResult(List<ValidationResult.Issue> issues) implements SemanticsResult {
}
