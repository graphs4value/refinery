package tools.refinery.store.query;

import java.util.UUID;

public final class DNFUtils {
	private DNFUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static String generateUniqueName(String originalName) {
		UUID uuid = UUID.randomUUID();
		String uniqueString = "_" + uuid.toString().replace('-', '_');
		if (originalName == null) {
			return uniqueString;
		} else {
			return originalName + uniqueString;
		}
	}
}
