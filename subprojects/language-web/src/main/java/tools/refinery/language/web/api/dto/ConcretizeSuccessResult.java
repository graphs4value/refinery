/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import org.eclipse.xtext.web.server.validation.ValidationResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ConcretizeSuccessResult(List<ValidationResult.Issue> issues, @Nullable JsonOutput json,
									  @Nullable String source) {
}
