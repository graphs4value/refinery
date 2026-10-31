/*
 * SPDX-FileCopyrightText: 2024-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests.internal;

import tools.refinery.generator.tests.TestCaseKind;

public record TestCaseHeader(TestCaseKind kind, String name) implements ChunkHeader {
}
