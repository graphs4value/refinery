/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

public final class PropagationRejectedException extends RuntimeException {
	private final transient Object reason;

	PropagationRejectedException(Object reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Object getReason() {
		return reason;
	}
}
