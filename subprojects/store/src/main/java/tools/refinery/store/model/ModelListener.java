package tools.refinery.store.model;

public interface ModelListener {
	default void beforeCommit() {
	}

	default void afterCommit() {
	}

	default void beforeRestore(long state) {
	}

	default void afterRestore() {
	}
}
