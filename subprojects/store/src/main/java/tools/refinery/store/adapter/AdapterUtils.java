/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.adapter;

import java.util.Collection;
import java.util.Optional;

public class AdapterUtils {
	private AdapterUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static <T, U extends T> Optional<U> tryGetAdapter(Collection<T> adapters, Class<? extends U> type) {
		var iterator = adapters.stream().filter(type::isInstance).iterator();
		if (!iterator.hasNext()) {
			return Optional.empty();
		}
		var adapter = type.cast(iterator.next());
		if (iterator.hasNext()) {
			throw new IllegalArgumentException("Ambiguous adapter: both %s and %s match %s"
					.formatted(adapter.getClass().getName(), iterator.next().getClass().getName(), type.getName()));
		}
		return Optional.of(adapter);
	}

	public static <T> T getAdapter(Collection<? super T> adapters, Class<T> type) {
		return tryGetAdapter(adapters, type).orElseThrow(() -> new IllegalArgumentException(
				"No %s adapter was configured".formatted(type.getName())));
	}
}
