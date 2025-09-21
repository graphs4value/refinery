/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.string;

import tools.refinery.logic.AbstractDomain;

// Singleton pattern, because there is only one domain for strings.
@SuppressWarnings("squid:S6548")
public class StringDomain implements AbstractDomain<StringValue, String> {
	public static final StringDomain INSTANCE = new StringDomain();

	@Override
	public Class<StringValue> abstractType() {
		return StringValue.class;
	}

	@Override
	public Class<String> concreteType() {
		return String.class;
	}

	@Override
	public StringValue unknown() {
		return StringValue.UNKNOWN;
	}

	@Override
	public StringValue error() {
		return StringValue.ERROR;
	}

	@Override
	public StringValue toAbstract(String concreteValue) {
		return StringValue.of(concreteValue);
	}
}
