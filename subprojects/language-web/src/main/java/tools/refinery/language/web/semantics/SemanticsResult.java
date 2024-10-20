/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.validation.ValidationResult;

import java.util.List;

public record SemanticsResult(SemanticsModelResult model, String error, boolean propagationRejected,
							  List<ValidationResult.Issue> issues) implements IServiceResult {
	public SemanticsResult(SemanticsModelResult model) {
		this(model, null, false, List.of());
	}

	public SemanticsResult(String error) {
		this(null, error, true, List.of());
	}

	public SemanticsResult(SemanticsModelResult model, boolean propagationRejected, String error) {
		this(model, error, propagationRejected, List.of());
	}

	public SemanticsResult(List<ValidationResult.Issue> issues) {
		this(null, null, true, issues);
	}

	public SemanticsResult(SemanticsModelResult model, boolean propagationRejected,
						   List<ValidationResult.Issue> issues) {
		this(model, null, propagationRejected, issues);
	}
}
