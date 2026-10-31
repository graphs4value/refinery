/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import org.jetbrains.annotations.Nullable;
import tools.refinery.generator.json.PartialModelJson;

public record GenerateSuccessResult(@Nullable PartialModelJson json, @Nullable String source) {
}
