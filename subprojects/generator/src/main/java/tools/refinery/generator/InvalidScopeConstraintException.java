/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;


public class InvalidScopeConstraintException extends IllegalArgumentException {
	public InvalidScopeConstraintException(String message) {
		super(message);
	}
}
