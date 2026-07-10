/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.theory;

public sealed interface TheorySupport {
	TheorySupport ENABLED_BY_DEFAULT = new EnabledByDefault();
	TheorySupport EXPLICIT_ONLY = DisabledByDefault.EXPLICIT_ONLY;
	TheorySupport UNSUPPORTED = DisabledByDefault.UNSUPPORTED;

	default boolean isSupported() {
		return true;
	}

	/**
	 * Computes the weaker preference between {@code this} and {@code other}.
	 *
	 * @param other The other preference.
	 * @return The weaker of the two preferences.
	 */
	default TheorySupport meet(TheorySupport other) {
		if (this instanceof EnabledByDefault(int preference) &&
				other instanceof EnabledByDefault(int otherPreference)) {
			return preference <= otherPreference ? this : other;
		}
		if (this == UNSUPPORTED || other == UNSUPPORTED) {
			return UNSUPPORTED;
		}
		return EXPLICIT_ONLY;
	}

	/**
	 * Denotes that the theory wishes to handle and expression by default.
	 * <p>
	 *     If the user does not configure a theory with the {@code using} keyword, the expression will be routed to
	 *     the theory with the highest {@code preference} value.
	 * </p>
	 * <p>
	 *     If there are multiple theories with the same {@code preference} value, the expression will be routed to
	 *     all of them. This can be used to create theory combinations from theories with different completeness
	 *     properties.
	 * </p>
	 *
	 * @param preference The theory preference. Higher values denote a stronger preference to handle an expression
	 *                   <i>instead of</i> other available theories. The default preference is {@code 0}.
	 */
	record EnabledByDefault(int preference) implements TheorySupport {
		public static final int DEFAULT_PREFERENCE = 0;

		public EnabledByDefault() {
			this(DEFAULT_PREFERENCE);
		}
	}

	enum DisabledByDefault implements TheorySupport {
		/**
		 * Denotes that a theory can handle expression, but does not with to do so by default.
		 */
		EXPLICIT_ONLY,

		/**
		 * Denotes that a theory cannot handle an expression.
		 */
		UNSUPPORTED {
			@Override
			public boolean isSupported() {
				return false;
			}
		};
	}
}
