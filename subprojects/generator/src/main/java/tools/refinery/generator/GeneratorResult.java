/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

public enum GeneratorResult {
	SUCCESS {
		@Override
		public void orThrow() {
			// No need to throw on error.
		}
	},
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

	public abstract void orThrow();
}
