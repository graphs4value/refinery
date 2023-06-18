/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

public interface Versioned {
	public long commit();
	//maybe revert()?
	public void restore(long state);
}
