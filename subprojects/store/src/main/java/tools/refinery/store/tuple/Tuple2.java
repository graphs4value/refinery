package tools.refinery.store.tuple;

public record Tuple2(int value0, int value1) implements Tuple {
	@Override
	public int getSize() {
		return 2;
	}

	@Override
	public int get(int element) {
		return switch (element) {
			case 0 -> value0;
			case 1 -> value1;
			default -> throw new ArrayIndexOutOfBoundsException(element);
		};
	}

	@Override
	public int[] toArray() {
		return new int[]{value0, value1};
	}

	@Override
	public String toString() {
		return "[" + value0 + ", " + value1 + "]";
	}
}
