/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import java.util.UUID;

public final class DnfUtils {
	private DnfUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static String generateUniqueName(String originalName) {
		UUID uuid = UUID.randomUUID();
		String uniqueString = "_" + uuid.toString().replace('-', '_');
		if (originalName == null) {
			return uniqueString;
		} else {
			return originalName + uniqueString;
		}
	}
}
