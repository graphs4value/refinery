/*******************************************************************************
 * Copyright (c) 2025 The Refinery Authors <https://refinery.tools/>
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * <p>
 * Portions of this file have been copied from
 * https://github.com/eclipse-xtext/xtext/blob/f9b6d1bebe09dc5775d45671d255384a8160015c/org.eclipse.xtext.util/src/org/eclipse/xtext/util/JavaStringConverter.java
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.logic.term.string;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.operators.Add;
import tools.refinery.logic.term.truthvalue.TruthValue;

public sealed interface StringValue extends AbstractValue<StringValue, String>, Add<StringValue> {
	StringValue UNKNOWN = Abstract.UNKNOWN;
	StringValue ERROR = Abstract.ERROR;

	static StringValue of(@Nullable String value) {
		return value == null ? ERROR : new Concrete(value);
	}

	enum Abstract implements StringValue {
		UNKNOWN {
			@Override
			public @NotNull String getArbitrary() {
				return "";
			}

			@Override
			public boolean isError() {
				return false;
			}

			@Override
			public StringValue join(StringValue other) {
				return UNKNOWN;
			}

			@Override
			public StringValue meet(StringValue other) {
				return other;
			}

			@Override
			public boolean isRefinementOf(StringValue other) {
				return other == UNKNOWN;
			}

			@Override
			public boolean isOverlapping(StringValue other) {
				return other != ERROR;
			}

			@Override
			public TruthValue checkEquals(StringValue other) {
				return other == ERROR ? TruthValue.ERROR : TruthValue.UNKNOWN;
			}

			@Override
			public StringValue add(StringValue other) {
				return UNKNOWN;
			}

			@Override
			public String toString() {
				return "unknown";
			}
		},
		ERROR {
			@Override
			public @Nullable String getArbitrary() {
				return null;
			}

			@Override
			public boolean isError() {
				return true;
			}

			@Override
			public StringValue join(StringValue other) {
				return other;
			}

			@Override
			public StringValue meet(StringValue other) {
				return ERROR;
			}

			@Override
			public boolean isRefinementOf(StringValue other) {
				return true;
			}

			@Override
			public boolean isOverlapping(StringValue other) {
				return false;
			}

			@Override
			public TruthValue checkEquals(StringValue other) {
				return TruthValue.ERROR;
			}

			@Override
			public StringValue add(StringValue other) {
				return ERROR;
			}

			@Override
			public String toString() {
				return "error";
			}
		};

		@Override
		public @Nullable String getConcrete() {
			return null;
		}

		@Override
		public boolean isConcrete() {
			return false;
		}
	}

	record Concrete(@NotNull String value) implements StringValue {
		@Override
		public @NotNull String getConcrete() {
			return value;
		}

		@Override
		public boolean isConcrete() {
			return true;
		}

		@Override
		public @NotNull String getArbitrary() {
			return getConcrete();
		}

		@Override
		public boolean isError() {
			return false;
		}

		@Override
		public StringValue join(StringValue other) {
			return other == ERROR || equals(other) ? this : UNKNOWN;
		}

		@Override
		public StringValue meet(StringValue other) {
			return isRefinementOf(other) ? this : ERROR;
		}

		@Override
		public boolean isRefinementOf(StringValue other) {
			return other == UNKNOWN || equals(other);
		}

		@Override
		public boolean isOverlapping(StringValue other) {
			return isRefinementOf(other);
		}

		@Override
		public StringValue add(StringValue other) {
			return switch (other) {
				case Abstract.UNKNOWN -> UNKNOWN;
				case Abstract.ERROR -> ERROR;
				case Concrete(var otherValue) -> new Concrete(value + otherValue);
			};
		}

		@Override
		public TruthValue checkEquals(StringValue other) {
			return switch (other) {
			case Abstract.UNKNOWN -> TruthValue.UNKNOWN;
			case Abstract.ERROR -> TruthValue.ERROR;
			case Concrete concrete -> equals(concrete) ? TruthValue.TRUE : TruthValue.FALSE;
			};
		}

		@Override
		public @NotNull String toString() {
			int length = value.length();
			StringBuilder result = new StringBuilder(length + 6);
			result.append('"');
			for (int i = 0; i < length; i++) {
				escapeAndAppendTo(value.charAt(i), result);
			}
			result.append('"');
			return result.toString();
		}

		/**
		 * Escapes control characters with a preceding backslash.
		 * <p>
		 * This method was copied from {@code org.eclipse.xtext.util.JavaStringConverter}.
		 * </p>
		 * @param c The character to escape and append to the result.
		 * @param result The {@link StringBuilder} to append to.
		 */
		private static void escapeAndAppendTo(char c, StringBuilder result) {
			String appendMe;
			switch (c) {
			case '\b':
				appendMe = "\\b";
				break;
			case '\t':
				appendMe = "\\t";
				break;
			case '\n':
				appendMe = "\\n";
				break;
			case '\f':
				appendMe = "\\f";
				break;
			case '\r':
				appendMe = "\\r";
				break;
			case '"':
				appendMe = "\\\"";
				break;
			case '\'':
				appendMe = "\\'";
				break;
			case '\\':
				appendMe = "\\\\";
				break;
			default:
				result.append(c);
				return;
			}
			result.append(appendMe);
		}
	}
}
