/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.statecoding.neighborhood.IndividualsSet;

import java.util.List;

public interface StateCodeCalculatorFactory {
	StateCodeCalculator create(Model model, List<? extends Interpretation<?>> interpretations,
                               IndividualsSet individuals);
}
