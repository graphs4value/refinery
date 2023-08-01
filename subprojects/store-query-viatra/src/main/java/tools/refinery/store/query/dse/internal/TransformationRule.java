package tools.refinery.store.query.dse.internal;

import org.eclipse.collections.api.block.procedure.Procedure;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.dse.ActionFactory;
import tools.refinery.store.query.resultset.OrderedResultSet;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.LinkedHashSet;
import java.util.Random;

public class TransformationRule {

	private final String name;
	private final RelationalQuery precondition;
	private final ActionFactory actionFactory;
	private Procedure<Tuple> action;
	private OrderedResultSet<Boolean> activations;
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
	public boolean prepare(Model model, ModelQueryAdapter queryEngine) {
		action = actionFactory.prepare(model);
		this.queryEngine = queryEngine;
		activations = new OrderedResultSet<>(queryEngine.getResultSet(precondition));
		return true;
	}

	public boolean fireActivation(Tuple activation) {
		action.accept(activation);
		queryEngine.flushChanges();
		return true;
	}

	public boolean fireRandomActivation() {
		return getRandomActivation().fire();
	}

	public String getName() {
		return name;
	}

	public RelationalQuery getPrecondition() {
		return precondition;
	}

	public ResultSet<Boolean> getAllActivationsAsSets() {
		return activations;
	}

	public LinkedHashSet<Activation> getAllActivations() {
		var result = new LinkedHashSet<Activation>();
		var cursor = activations.getAll();
		while (cursor.move()) {
			result.add(new Activation(this, cursor.getKey()));
		}
		return result;
	}

	public Activation getRandomActivation() {
		return new Activation(this, activations.getKey(random.nextInt(activations.size())));
	}

	public Activation getActivation(int index) {
		return new Activation(this, activations.getKey(index));
	}
}
