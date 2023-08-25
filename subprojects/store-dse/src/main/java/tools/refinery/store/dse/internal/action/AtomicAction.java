/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public interface AtomicAction {

	void fire(Tuple activation);

	AtomicAction prepare(Model model);

	boolean equalsWithSubstitution(AtomicAction other);
}
