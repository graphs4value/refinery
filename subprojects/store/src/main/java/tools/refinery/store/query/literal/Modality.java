package tools.refinery.store.query.literal;

import java.util.Locale;

public enum Modality {
	MUST,
	MAY,
	CURRENT;

	public Modality negate() {
		return switch(this) {
			case MUST -> MAY;
			case MAY -> MUST;
			case CURRENT -> CURRENT;
		};
	}

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
