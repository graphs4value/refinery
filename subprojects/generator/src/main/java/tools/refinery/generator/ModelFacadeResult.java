/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.store.dse.propagation.PropagationRejectedResult;

public sealed interface ModelFacadeResult {
	ModelFacadeResult SUCCESS = new Success();

	boolean isPropagationRejected();

	boolean isConcretizationRejected();

	default boolean isRejected() {
		return isPropagationRejected() || isConcretizationRejected();
	}

	void throwIfRejected();

	final class Success implements ModelFacadeResult {
		private Success() {
		}

		@Override
		public boolean isPropagationRejected() {
			return false;
		}

		@Override
		public boolean isConcretizationRejected() {
			return false;
		}

		@Override
		public void throwIfRejected() {
			// Nothing to throw.
		}
	}

	sealed interface Rejected extends ModelFacadeResult {
		String formatMessage();

		@Override
		default void throwIfRejected() {
			throw new IllegalStateException(formatMessage());
		}

		Object reason();
	}

	record PropagationRejected(PropagationRejectedResult propagationResult) implements Rejected {
		@Override
		public boolean isPropagationRejected() {
			return true;
		}

		@Override
		public boolean isConcretizationRejected() {
			return false;
		}

		@Override
		public String formatMessage() {
			return propagationResult.formatMessage();
		}

		@Override
		public Object reason() {
			return propagationResult.reason();
		}
	}

	record ConcretizationRejected(PropagationRejectedResult concretizationResult) implements Rejected {
		@Override
		public boolean isPropagationRejected() {
			return false;
		}

		@Override
		public boolean isConcretizationRejected() {
			return true;
		}

		@Override
		public String formatMessage() {
			return "Concretization failed: %s".formatted(concretizationResult.message());
		}

		@Override
		public Object reason() {
			return concretizationResult.reason();
		}
	}
}
