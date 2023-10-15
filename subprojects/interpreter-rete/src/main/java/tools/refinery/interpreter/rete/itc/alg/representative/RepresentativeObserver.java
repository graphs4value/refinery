/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.rete.itc.alg.representative;

import tools.refinery.interpreter.matchers.util.Direction;

public interface RepresentativeObserver<T> {
	void tupleChanged(T node, T representative, Direction direction);
}
