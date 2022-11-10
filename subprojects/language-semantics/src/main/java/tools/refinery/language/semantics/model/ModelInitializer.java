package tools.refinery.language.semantics.model;

import com.google.inject.Inject;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.model.internal.DecisionTree;
import tools.refinery.language.utils.ProblemDesugarer;
import tools.refinery.language.utils.RelationInfo;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.HashMap;
import java.util.Map;

public class ModelInitializer {
	@Inject
	private ProblemDesugarer desugarer;

	private final MutableObjectIntMap<Node> nodeTrace = ObjectIntMaps.mutable.empty();

	private final Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationTrace =
			new HashMap<>();

	private int nodeCount = 0;

	public void createModel(Problem problem) {
		var builtinSymbols = desugarer.getBuiltinSymbols(problem).orElseThrow(() -> new IllegalArgumentException(
				"Problem has no builtin library"));
		var collectedSymbols = desugarer.collectSymbols(problem);
		for (var node : collectedSymbols.nodes().keySet()) {
			nodeTrace.put(node, nodeCount);
			nodeCount += 1;
		}
		for (var pair : collectedSymbols.relations().entrySet()) {
			var relation = pair.getKey();
			var relationInfo = pair.getValue();
			var isEqualsRelation = relation == builtinSymbols.equals();
			var decisionTree = mergeAssertions(relationInfo, isEqualsRelation);
			var defaultValue = isEqualsRelation ? TruthValue.FALSE : TruthValue.UNKNOWN;
			relationTrace.put(relation, new Relation<>(relationInfo.name(), relationInfo.arity(), TruthValue.class, defaultValue
			));
		}
	}

	private DecisionTree mergeAssertions(RelationInfo relationInfo, boolean isEqualsRelation) {
		var arity = relationInfo.arity();
		var defaultAssertions = new DecisionTree(arity, isEqualsRelation ? null : TruthValue.UNKNOWN);
		var assertions = new DecisionTree(arity);
		for (var assertion : relationInfo.assertions()) {
			var tuple = getTuple(assertion);
			var value = getTruthValue(assertion.getValue());
			if (assertion.isDefault()) {
				defaultAssertions.mergeValue(tuple, value);
			} else {
				assertions.mergeValue(tuple, value);
			}
		}
		defaultAssertions.overwriteValues(assertions);
		if (isEqualsRelation) {
			for (int i = 0; i < nodeCount; i++) {
				defaultAssertions.setIfMissing(Tuple.of(i, i), TruthValue.TRUE);
			}
			defaultAssertions.setAllMissing(TruthValue.FALSE);
		}
		return defaultAssertions;
	}

	private Tuple getTuple(Assertion assertion) {
		var arguments = assertion.getArguments();
		int arity = arguments.size();
		var nodes = new int[arity];
		for (int i = 0; i < arity; i++) {
			var argument = arguments.get(i);
			if (argument instanceof NodeAssertionArgument nodeArgument) {
				nodes[i] = nodeTrace.getOrThrow(nodeArgument.getNode());
			} else if (argument instanceof ConstantAssertionArgument constantArgument) {
				nodes[i] = nodeTrace.getOrThrow(constantArgument.getNode());
			} else if (argument instanceof WildcardAssertionArgument) {
				nodes[i] = -1;
			} else {
				throw new IllegalArgumentException("Unknown assertion argument: " + argument);
			}
		}
		return Tuple.of(nodes);
	}

	private static TruthValue getTruthValue(LogicValue value) {
		return switch (value) {
			case TRUE -> TruthValue.TRUE;
			case FALSE -> TruthValue.FALSE;
			case UNKNOWN -> TruthValue.UNKNOWN;
			case ERROR -> TruthValue.ERROR;
		};
	}
}
