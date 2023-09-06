/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.NodeVariable;

import java.util.List;

public interface ActionLiteral {
	List<NodeVariable> getInputVariables();

	List<NodeVariable> getOutputVariables();

	BoundActionLiteral bindToModel(Model model);
}
