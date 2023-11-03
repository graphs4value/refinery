/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

public class UnsatisfiableProblemException extends RuntimeException {
	public UnsatisfiableProblemException() {
		super("Model generation problem was unsatisfiable");
	}
}
