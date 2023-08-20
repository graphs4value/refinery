/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query;

public class InvalidQueryException extends RuntimeException {
	public InvalidQueryException() {
	}

	public InvalidQueryException(String message) {
		super(message);
	}

	public InvalidQueryException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidQueryException(Throwable cause) {
		super(cause);
	}
}
