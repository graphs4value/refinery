/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

/**
 * Object that can save and restore its state.
 */
public interface Versioned {
	/**
	 * Saves the state of the object.
	 * @return an object that marks the version of the object at the time the function was called.
	 */
	Version commit();

	/**
	 * Restores the state of the object.
	 * @param state a {@link Version} object that marks the version. The state must be a {@link Version} object
	 *                 returned by a previous {@link #commit()}!
	 */
	void restore(Version state);
}
