/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

public class UnsatisfiableProblemException extends RuntimeException {
	public static final String DEFAULT_MESSAGE = "Model generation problem is unsatisfiable";

	public UnsatisfiableProblemException() {
		this(DEFAULT_MESSAGE);
	}

	public UnsatisfiableProblemException(String message) {
		super(message);
	}
}
