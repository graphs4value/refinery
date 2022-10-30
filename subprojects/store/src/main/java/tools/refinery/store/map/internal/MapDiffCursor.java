package tools.refinery.store.map.internal;

import java.util.List;
import java.util.stream.Stream;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMap;

/**
 * A cursor representing the difference between two states of a map.
 *
 * @author Oszkar Semerath
 *
 */
public class MapDiffCursor<K, V> implements DiffCursor<K, V>, Cursor<K, V> {
	/**
	 * Default nodeId representing missing elements.
	 */
	private V defaultValue;
	private MapCursor<K, V> cursor1;
	private MapCursor<K, V> cursor2;
	private ContinousHashProvider<? super K> hashProvider;

	// Values
	private K key;
	private V fromValue;
	private V toValue;

	// State
	/**
	 * Positive number if cursor 1 is behind, negative number if cursor 2 is behind,
	 * and 0 if they are at the same position.
	 */
	private int cursorRelation;
	private HashClash hashClash = HashClash.NONE;

	public MapDiffCursor(ContinousHashProvider<? super K> hashProvider, V defaultValue, Cursor<K, V> cursor1,
			Cursor<K, V> cursor2) {
		super();
		this.hashProvider = hashProvider;
		this.defaultValue = defaultValue;
		this.cursor1 = (MapCursor<K, V>) cursor1;
		this.cursor2 = (MapCursor<K, V>) cursor2;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getFromValue() {
		return fromValue;
	}

	@Override
	public V getToValue() {
		return toValue;
	}

	@Override
	public V getValue() {
		return getToValue();
	}

	public boolean isTerminated() {
		return cursor1.isTerminated() && cursor2.isTerminated();
	}

	@Override
	public boolean isDirty() {
		return this.cursor1.isDirty() || this.cursor2.isDirty();
	}

	@Override
	public List<VersionedMap<?, ?>> getDependingMaps() {
		return Stream.concat(cursor1.getDependingMaps().stream(), cursor2.getDependingMaps().stream()).toList();
	}

	protected void updateState() {
		if (!isTerminated()) {
			this.cursorRelation = MapCursor.compare(cursor1, cursor2);
			if (cursorRelation > 0 || cursor2.isTerminated()) {
				this.key = cursor1.getKey();
				this.fromValue = cursor1.getValue();
				this.toValue = defaultValue;
			} else if (cursorRelation < 0 || cursor1.isTerminated()) {
				this.key = cursor2.getKey();
				this.fromValue = defaultValue;
				this.toValue = cursor1.getValue();
			} else {
				// cursor1 = cursor2
				if (cursor1.getKey().equals(cursor2.getKey())) {
					this.key = cursor1.getKey();
					this.fromValue = cursor1.getValue();
					this.toValue = defaultValue;
				} else {
					resolveHashClashWithFirstEntry();
				}
			}
		}
	}

	protected void resolveHashClashWithFirstEntry() {
		int compareResult = this.hashProvider.compare(cursor1.key, cursor2.key);
		if (compareResult < 0) {
			this.hashClash = HashClash.STUCK_CURSOR_2;
			this.cursorRelation = 0;
			this.key = cursor1.key;
			this.fromValue = cursor1.value;
			this.toValue = defaultValue;
		} else if (compareResult > 0) {
			this.hashClash = HashClash.STUCK_CURSOR_1;
			this.cursorRelation = 0;
			this.key = cursor2.key;
			this.fromValue = defaultValue;
			this.toValue = cursor2.value;
		} else {
			throw new IllegalArgumentException("Inconsistent compare result for diffcursor");
		}
	}

	protected boolean isInHashClash() {
		return this.hashClash != HashClash.NONE;
	}

	protected void resolveHashClashWithSecondEntry() {
		switch (this.hashClash) {
		case STUCK_CURSOR_1:
			this.hashClash = HashClash.NONE;
			this.cursorRelation = 0;
			this.key = cursor1.key;
			this.fromValue = cursor1.value;
			this.toValue = defaultValue;
			break;
		case STUCK_CURSOR_2:
			this.hashClash = HashClash.NONE;
			this.cursorRelation = 0;
			this.key = cursor2.key;
			this.fromValue = defaultValue;
			this.toValue = cursor2.value;
			break;
		default:
			throw new IllegalArgumentException("Inconsistent compare result for diffcursor");
		}
	}

	protected boolean sameValues() {
		if (this.fromValue == null) {
			return this.toValue == null;
		} else {
			return this.fromValue.equals(this.toValue);
		}
	}

	protected boolean moveOne() {
		if (isTerminated()) {
			return false;
		}
		if (this.cursorRelation > 0 || cursor2.isTerminated()) {
			return cursor1.move();
		} else if (this.cursorRelation < 0 || cursor1.isTerminated()) {
			return cursor2.move();
		} else {
			boolean moved1 = cursor1.move();
			boolean moved2 = cursor2.move();
			return moved1 && moved2;
		}
	}

	private boolean skipNode() {
		if (isTerminated()) {
			throw new IllegalStateException("DiffCursor tries to skip when terminated!");
		}
		boolean update1 = cursor1.skipCurrentNode();
		boolean update2 = cursor2.skipCurrentNode();
		updateState();
		return update1 && update2;
	}

	protected boolean moveToConsistentState() {
		if (!isTerminated()) {
			boolean changed;
			boolean lastResult = true;
			do {
				changed = false;
				if (MapCursor.sameSubnode(cursor1, cursor2)) {
					lastResult = skipNode();
					changed = true;
				}
				if (sameValues()) {
					lastResult = moveOne();
					changed = true;
				}
				updateState();
			} while (changed && !isTerminated());
			return lastResult;
		} else {
			return false;
		}
	}

	public boolean move() {
		if (!isTerminated()) {
			if (isInHashClash()) {
				this.resolveHashClashWithSecondEntry();
				return true;
			} else {
				if (moveOne()) {
					return moveToConsistentState();
				} else {
					return false;
				}
			}

		} else
			return false;
	}
}
