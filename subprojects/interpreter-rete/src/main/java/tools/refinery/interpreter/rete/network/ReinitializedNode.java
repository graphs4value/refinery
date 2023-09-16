/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.rete.network;

import tools.refinery.interpreter.matchers.tuple.Tuple;

import java.util.Collection;

public interface ReinitializedNode {
	void reinitializeWith(Collection<Tuple> tuples);
}
