/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

public sealed interface PropagationResult permits PropagationSuccessResult, PropagationRejectedResult {
	PropagationResult UNCHANGED = PropagationSuccessResult.UNCHANGED;
	PropagationResult PROPAGATED = PropagationSuccessResult.PROPAGATED;

	PropagationResult andThen(PropagationResult next);

	boolean isRejected();

	void throwIfRejected();

	boolean isChanged();
}
