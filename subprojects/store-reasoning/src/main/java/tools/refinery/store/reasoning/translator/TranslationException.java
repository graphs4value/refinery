/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.store.reasoning.representation.AnyPartialSymbol;

public class TranslationException extends RuntimeException {
	private final transient AnyPartialSymbol partialSymbol;

	public TranslationException(AnyPartialSymbol partialSymbol) {
		this.partialSymbol = partialSymbol;
	}

	public TranslationException(AnyPartialSymbol partialSymbol, String message) {
		super(message);
		this.partialSymbol = partialSymbol;
	}

	public TranslationException(AnyPartialSymbol partialSymbol, String message, Throwable cause) {
		super(message, cause);
		this.partialSymbol = partialSymbol;
	}

	public TranslationException(AnyPartialSymbol partialSymbol, Throwable cause) {
		super(cause);
		this.partialSymbol = partialSymbol;
	}

	public AnyPartialSymbol getPartialSymbol() {
		return partialSymbol;
	}
}
