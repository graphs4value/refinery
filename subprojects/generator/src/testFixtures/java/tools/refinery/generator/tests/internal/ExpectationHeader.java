/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests.internal;

import tools.refinery.store.reasoning.literal.Concreteness;

public record ExpectationHeader(Concreteness concreteness, boolean exact, String description,
								int startLine) implements ChunkHeader {
}
