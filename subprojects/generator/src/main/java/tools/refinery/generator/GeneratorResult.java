/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

public enum GeneratorResult {
	REQUEST_FULFILLED,
	NO_MORE_SOLUTIONS,
	UNSATISFIABLE {
		@Override
		public void orThrow() {
			throw new UnsatisfiableProblemException();
		}
	},
	TIMEOUT {
		@Override
		public void orThrow() {
			throw new GeneratorTimeoutException();
		}
	};

	public void orThrow() {
		// No need to throw if there was no error.
	}
}
