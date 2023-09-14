/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

public enum ParameterDirection {
	OUT("out"),
	IN("in");

	private final String name;

	ParameterDirection(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
