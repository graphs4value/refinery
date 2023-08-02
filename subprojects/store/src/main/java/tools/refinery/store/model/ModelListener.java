/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.map.Version;

public interface ModelListener {
	default void beforeCommit() {
	}

	default void afterCommit() {
	}

	default void beforeRestore(Version state) {
	}

	default void afterRestore() {
	}
}
