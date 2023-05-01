/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.exceptions;

public class IncompatibleParameterDirectionException extends RuntimeException {
	public IncompatibleParameterDirectionException(String message) {
		super(message);
	}

	public IncompatibleParameterDirectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
