/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import org.jetbrains.annotations.Nullable;

public record GenerateSuccessResult(@Nullable JsonOutput json, @Nullable String source) {
}
