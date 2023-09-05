/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.dse.ActionFactory;
import tools.refinery.store.query.resultset.OrderedResultSet;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

public class TransformationRule {

	private final String name;
	private final RelationalQuery precondition;
	private final ActionFactory actionFactory;

	private Random random;
	private ModelQueryAdapter queryEngine;

	public TransformationRule(String name, RelationalQuery precondition, ActionFactory actionFactory) {
		this(name, precondition, actionFactory, new Random());
	}

	public TransformationRule(String name, RelationalQuery precondition, ActionFactory actionFactory, long seed) {
		this(name, precondition, actionFactory, new Random(seed));
	}

	public TransformationRule(String name, RelationalQuery precondition, ActionFactory actionFactory, Random random) {
		this.name = name;
		this.precondition = precondition;
		this.actionFactory = actionFactory;
		this.random = random;
	}
	public void doConfigure(ModelStoreBuilder storeBuilder) {
		var queryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		queryBuilder.query(this.precondition);
	}

	public Transformation prepare(Model model) {
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var activations = new OrderedResultSet<>(queryEngine.getResultSet(precondition));
		var action = actionFactory.prepare(model);
		return new Transformation(this,activations,action);
	}

	public String getName() {
		return name;
	}

	public RelationalQuery getPrecondition() {
		return precondition;
	}

}
