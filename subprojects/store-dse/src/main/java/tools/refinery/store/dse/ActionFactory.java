/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import org.eclipse.collections.api.block.procedure.Procedure;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public interface ActionFactory {
	Procedure<Tuple> prepare(Model model);
}
