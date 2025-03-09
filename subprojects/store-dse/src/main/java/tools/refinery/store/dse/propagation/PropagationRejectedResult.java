/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

public record PropagationRejectedResult(Object reason, String message, boolean fatal) implements PropagationResult {
	public PropagationRejectedResult(Object reason, String message) {
		this(reason, message, false);
	}

	@Override
	public PropagationResult andThen(PropagationResult next) {
		return this;
	}

	@Override
	public boolean isRejected() {
		return true;
	}

	@Override
	public void throwIfRejected() {
		throw new PropagationRejectedException(reason, formatMessage());
	}

	public String formatMessage() {
		return "Propagation failed: %s".formatted(message);
	}

	@Override
	public boolean isChanged() {
		return false;
	}
}
