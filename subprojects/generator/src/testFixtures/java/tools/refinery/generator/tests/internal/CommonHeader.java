/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests.internal;

public final class CommonHeader implements ChunkHeader {
	public static final CommonHeader INSTANCE = new CommonHeader();

	private CommonHeader() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[]";
	}
}
