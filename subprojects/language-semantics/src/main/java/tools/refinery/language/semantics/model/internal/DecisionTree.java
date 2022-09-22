package tools.refinery.language.semantics.model.internal;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.TruthValue;

public class DecisionTree {
	private final int levels;

	private final DecisionTreeNode root;

	public DecisionTree(int levels, TruthValue initialValue) {
		this.levels = levels;
		DecisionTreeNode node = new TerminalNode(IntObjectMaps.mutable.empty(),
				DecisionTreeValue.fromTruthValue(initialValue));
		for (int level = 1; level < levels; level++) {
			node = new IntermediateNode(IntObjectMaps.mutable.empty(), node);
		}
		root = node;
	}

	public TruthValue get(Tuple tuple) {
		return root.getValue(levels - 1, tuple).getTruthValue();
	}

	public void mergeValue(Tuple tuple, TruthValue truthValue) {
		if (truthValue == null) {
			return;
		}
		root.mergeValue(levels - 1, tuple, truthValue);
	}

	public void overwriteValues(DecisionTree values) {
		root.overwriteValues(values.root);
	}

	public Cursor<Tuple, TruthValue> getCursor(TruthValue defaultValue, int nodeCount) {
		return new DecisionTreeCursor(levels, defaultValue, nodeCount, root);
	}
}
