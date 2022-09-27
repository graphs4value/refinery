package tools.refinery.store.tuple;

public record Tuple0() implements Tuple {
	public static Tuple0 INSTANCE = new Tuple0();

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public int get(int element) {
		throw new IndexOutOfBoundsException(element);
	}

	@Override
	public int[] toArray() {
		return new int[]{};
	}

	@Override
	public String toString() {
		return "[]";
	}
}
