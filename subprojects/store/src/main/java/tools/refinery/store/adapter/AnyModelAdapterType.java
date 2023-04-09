/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.adapter;

import java.util.Collection;

public sealed interface AnyModelAdapterType permits ModelAdapterType {
	Class<? extends ModelAdapter> getModelAdapterClass();

	Class<? extends ModelStoreAdapter> getModelStoreAdapterClass();

	Class<? extends ModelAdapterBuilder> getModelAdapterBuilderClass();

	Collection<AnyModelAdapterType> getSupportedAdapterTypes();

	default boolean supports(AnyModelAdapterType targetAdapter) {
		return getSupportedAdapterTypes().contains(targetAdapter);
	}

	String getName();
}
