/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import org.jetbrains.annotations.Nullable;
import tools.refinery.generator.GeneratorResult;

import java.util.List;

public record GenerateManySuccessResult(GeneratorResult stopReason, @Nullable List<GenerateSuccessResult> models) {
}
