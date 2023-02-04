package tools.refinery.store.map.internal;

enum HashClash {
	/**
	 * Not stuck.
	 */
	NONE,

	/**
	 * Clashed, next we should return the key of cursor 1.
	 */
	STUCK_CURSOR_1,

	/**
	 * Clashed, next we should return the key of cursor 2.
	 */
	STUCK_CURSOR_2
}
