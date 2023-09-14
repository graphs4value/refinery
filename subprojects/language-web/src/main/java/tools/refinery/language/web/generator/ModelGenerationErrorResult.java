/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.generator;

import java.util.UUID;

public record ModelGenerationErrorResult(UUID uuid, String error) implements ModelGenerationResult {
}
