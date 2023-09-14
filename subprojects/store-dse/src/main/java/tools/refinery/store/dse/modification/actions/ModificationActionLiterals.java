/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.modification.actions;

import tools.refinery.store.dse.modification.DanglingEdges;
import tools.refinery.store.query.term.NodeVariable;

public class ModificationActionLiterals {
	private ModificationActionLiterals() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static CreateActionLiteral create(NodeVariable variable) {
		return new CreateActionLiteral(variable);
	}

	public static DeleteActionLiteral delete(NodeVariable variable, DanglingEdges danglingEdges) {
		return new DeleteActionLiteral(variable, danglingEdges);
	}
}
