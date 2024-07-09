/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.store.util.CancellationToken;

class CancellableCancellationToken implements CancellationToken {
	private volatile boolean cancelled;

	private final CancellationToken wrappedToken;

	public CancellableCancellationToken(CancellationToken wrappedToken) {
		this.wrappedToken = wrappedToken;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		cancelled = true;
	}

	public void reset() {
		cancelled = false;
	}

	@Override
	public void checkCancelled() {
		wrappedToken.checkCancelled();
		if (cancelled) {
			throw new GeneratorTimeoutException();
		}
	}
}
