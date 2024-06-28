/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.seed;

import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Model;

public record PropagatedModel(Model model, PropagationResult propagationResult) {
}
