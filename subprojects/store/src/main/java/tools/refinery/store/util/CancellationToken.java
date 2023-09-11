/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.util;

@FunctionalInterface
public interface CancellationToken {
	CancellationToken NONE = () -> {};

	void checkCancelled();
}
