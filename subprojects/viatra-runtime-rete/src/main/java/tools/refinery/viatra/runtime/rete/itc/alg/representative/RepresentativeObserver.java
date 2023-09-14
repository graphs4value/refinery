/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.viatra.runtime.rete.itc.alg.representative;

import tools.refinery.viatra.runtime.matchers.util.Direction;

public interface RepresentativeObserver {
	void tupleChanged(Object node, Object representative, Direction direction);
}
