/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf.callback;

import tools.refinery.logic.dnf.FunctionalQueryBuilder;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;

@FunctionalInterface
public interface FunctionalQueryCallback1<T> {
	void accept(FunctionalQueryBuilder<T> builder, NodeVariable p1, DataVariable<T> output);
}
