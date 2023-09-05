/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import org.eclipse.collections.api.block.procedure.Procedure;
import tools.refinery.store.query.resultset.OrderedResultSet;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

public class Transformation {
	private final TransformationRule definition;

	private final OrderedResultSet<Boolean> activations;

	private final Procedure<Tuple> action;

	public Transformation(TransformationRule definition, OrderedResultSet<Boolean> activations, Procedure<Tuple> action) {
		this.definition = definition;
		this.activations = activations;
		this.action = action;
	}

	public TransformationRule getDefinition() {
		return definition;
	}

	public ResultSet<Boolean> getAllActivationsAsResultSet() {
		return activations;
	}

	public Tuple getActivation(int index) {
		return activations.getKey(index);
	}

	public boolean fireActivation(Tuple activation) {
		action.accept(activation);
		//queryEngine.flushChanges();
		return true;
	}
}
