package tools.refinery.store.tuple;

import java.util.ArrayList;

public record Tuple1(int value0) implements Tuple {
	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public int get(int element) {
		if (element == 0) {
			return value0;
		}
		throw new IndexOutOfBoundsException(element);
	}

	@Override
	public int[] toArray() {
		return new int[]{value0};
	}

	@Override
	public String toString() {
		return "[" + value0 + "]";
	}

	public static class Cache {
		public static final Cache INSTANCE = new Cache();

		private final ArrayList<Tuple1> tuple1Cache = new ArrayList<>(1024);

		private Cache() {
		}

		public Tuple1 getOrCreate(int value) {
			if (value < 0) {
				// Mask tuple for QueryableModelStore, doesn't refer to a model node.
				return new Tuple1(value);
			}
			if (value < tuple1Cache.size()) {
				return tuple1Cache.get(value);
			}
			Tuple1 newlyCreated = null;
			tuple1Cache.ensureCapacity(value);
			while (value >= tuple1Cache.size()) {
				newlyCreated = new Tuple1(tuple1Cache.size());
				tuple1Cache.add(newlyCreated);
			}
			return newlyCreated;
		}
	}
}
