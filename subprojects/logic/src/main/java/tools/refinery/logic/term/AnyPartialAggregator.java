/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.AnyAbstractDomain;

public sealed interface AnyPartialAggregator permits PartialAggregator {
	AnyAbstractDomain getBodyDomain();

	AnyAbstractDomain getResultDomain();
}
