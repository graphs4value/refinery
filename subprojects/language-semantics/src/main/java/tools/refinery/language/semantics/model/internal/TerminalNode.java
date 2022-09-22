package tools.refinery.language.semantics.model.internal;

import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.TruthValue;

class TerminalNode extends DecisionTreeNode {
	private MutableIntObjectMap<DecisionTreeValue> children;

	private DecisionTreeValue otherwise;

	TerminalNode(MutableIntObjectMap<DecisionTreeValue> children, DecisionTreeValue otherwise) {
		this.children = children;
		this.otherwise = otherwise;
	}

	private DecisionTreeValue getChild(int index) {
		var child = children.get(index);
		return child == null ? otherwise : child;
	}

	@Override
	public DecisionTreeValue getValue(int level, Tuple tuple) {
		assertLevel(level);
		return getChild(tuple.get(level));
	}

	@Override
	public DecisionTreeNode deepCopy() {
		return new TerminalNode(IntObjectMaps.mutable.ofAll(children), otherwise);
	}

	@Override
	public void mergeValue(int level, Tuple tuple, TruthValue value) {
		assertLevel(level);
		super.mergeValue(level, tuple, value);
	}

	@Override
	protected void mergeAllValues(int nextLevel, Tuple tuple, TruthValue value) {
		otherwise = otherwise.merge(value);
		children = IntObjectMaps.mutable.from(children.keyValuesView(), IntObjectPair::getOne,
				pair -> pair.getTwo().merge(value));
		reduceChildren();
	}

	@Override
	protected void mergeSingleValue(int key, int nextLevel, Tuple tuple, TruthValue value) {
		var newChild = getChild(key).merge(value);
		if (newChild == otherwise) {
			children.remove(key);
		} else {
			children.put(key, newChild);
		}
	}

	@Override
	public void overwriteValues(DecisionTreeNode values) {
		if (values instanceof TerminalNode terminalValues) {
			otherwise = otherwise.overwrite(terminalValues.otherwise);
			children = IntObjectMaps.mutable.from(children.keyValuesView(), IntObjectPair::getOne,
					pair -> pair.getTwo().overwrite(terminalValues.getChild(pair.getOne())));
			for (var pair : terminalValues.children.keyValuesView()) {
				children.getIfAbsentPut(pair.getOne(), otherwise.overwrite(pair.getTwo()));
			}
			reduceChildren();
		} else {
			throw new IllegalArgumentException("Level mismatch");
		}
	}

	private void reduceChildren() {
		var iterator = children.iterator();
		while (iterator.hasNext()) {
			var child = iterator.next();
			if (child == otherwise) {
				iterator.remove();
			}
		}
	}

	@Override
	public boolean moveNext(int level, DecisionTreeCursor cursor) {
		assertLevel(level);
		return super.moveNext(level, cursor);
	}

	@Override
	protected DecisionTreeValue getOtherwiseReducedValue() {
		return otherwise;
	}

	@Override
	protected LazyIntIterable getChildKeys() {
		return children.keysView();
	}

	@Override
	protected boolean moveNextSparse(int level, DecisionTreeCursor cursor, int startIndex, int[] sortedChildren) {
		if (startIndex >= sortedChildren.length) {
			return false;
		}
		var key = sortedChildren[startIndex];
		cursor.iterationState[level] = startIndex;
		cursor.rawTuple[level] = key;
		cursor.value = getChild(key);
		return true;
	}

	@Override
	protected boolean moveNextDense(int level, DecisionTreeCursor cursor, int startIndex) {
		if (startIndex >= cursor.nodeCount) {
			return false;
		}
		cursor.iterationState[level] = startIndex;
		cursor.rawTuple[level] = startIndex;
		cursor.value = getChild(startIndex);
		return true;
	}

	private static void assertLevel(int level) {
		if (level != 0) {
			throw new IllegalArgumentException("Invalid decision tree level: " + level);
		}
	}
}
