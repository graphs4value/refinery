/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf.callback;

import tools.refinery.store.query.dnf.FunctionalQueryBuilder;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.NodeVariable;

@FunctionalInterface
public interface FunctionalQueryCallback1<T> {
	void accept(FunctionalQueryBuilder<T> builder, NodeVariable p1, DataVariable<T> output);
}
