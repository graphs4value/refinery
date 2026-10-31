/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests.internal;

public interface ChunkAcceptor {
	void acceptChunk(ChunkHeader header, String body);

	void acceptEnd();
}
