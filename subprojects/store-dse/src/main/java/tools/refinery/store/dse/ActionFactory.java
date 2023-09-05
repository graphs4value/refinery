/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

import java.util.function.Consumer;

public interface ActionFactory {
	Consumer<Tuple> prepare(Model model);
}
