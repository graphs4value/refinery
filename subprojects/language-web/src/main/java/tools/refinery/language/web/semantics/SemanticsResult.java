/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.validation.ValidationResult;

import java.util.List;

public record SemanticsResult(SemanticsModelResult model, String error,
							  List<ValidationResult.Issue> issues) implements IServiceResult {
	public SemanticsResult(SemanticsModelResult model) {
		this(model, null, List.of());
	}

	public SemanticsResult(String error) {
		this(null, error, List.of());
	}

	public SemanticsResult(SemanticsModelResult model, String error) {
		this(model, error, List.of());
	}

	public SemanticsResult(List<ValidationResult.Issue> issues) {
		this(null, null, issues);
	}
}
