/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.generator;

import org.eclipse.xtext.web.server.IServiceResult;

import java.util.UUID;

public record ModelGenerationStartedResult(UUID uuid) implements IServiceResult {
}
