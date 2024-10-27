/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import java.util.UUID;

public final class DnfUtils {

	//A konstruktora hibát dob.
	private DnfUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}


	public static String generateUniqueName(String originalName) {
		//UUID: immutable universally unique identifier (128 bit). A random UUID az cryptographically strong random
		// number generator-t használ.
		UUID uuid = UUID.randomUUID();
		//_ karakterhez hozzáfűzi a UUID-t, és a kötőjelet aláhúzásra cseréli benne.
		String uniqueString = "_" + uuid.toString().replace('-', '_');
		//Ha a paraméter null akkor a fenti stringgel tér vissza, ha nem akkor az eredeti névvel összefűzi a stringet.
		if (originalName == null) {
			return uniqueString;
		} else {
			return originalName + uniqueString;
		}
	}
}
