package tools.refinery.store.reasoning;

public enum MergeResult {
	UNCHANGED,
	REFINED,
	REJECTED;

	public MergeResult andAlso(MergeResult other) {
		return switch (this) {
			case UNCHANGED -> other;
			case REFINED -> other == REJECTED ? REJECTED : REFINED;
			case REJECTED -> REJECTED;
		};
	}
}
