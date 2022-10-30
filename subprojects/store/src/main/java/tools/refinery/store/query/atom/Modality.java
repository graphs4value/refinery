package tools.refinery.store.query.atom;

import java.util.Locale;

public enum Modality {
	MUST,
	MAY,
	CURRENT;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
