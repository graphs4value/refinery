/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf.callback;

import tools.refinery.logic.dnf.FunctionalQueryBuilder;
import tools.refinery.logic.term.DataVariable;

@FunctionalInterface
public interface FunctionalQueryCallback0<T> {
	void accept(FunctionalQueryBuilder<T> builder, DataVariable<T> output);
}
