/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import org.eclipse.emf.ecore.EObject;

public class TracedException extends RuntimeException {
	private final transient EObject sourceElement;

	public TracedException(EObject sourceElement) {
		this.sourceElement = sourceElement;
	}

	public TracedException(EObject sourceElement, String message) {
		super(message);
		this.sourceElement = sourceElement;
	}

	public TracedException(EObject sourceElement, String message, Throwable cause) {
		super(message, cause);
		this.sourceElement = sourceElement;
	}

	public TracedException(EObject sourceElement, Throwable cause) {
		super(cause);
		this.sourceElement = sourceElement;
	}

	public EObject getSourceElement() {
		return sourceElement;
	}

	@Override
	public String getMessage() {
		var message = super.getMessage();
		if (message == null) {
			return "Internal error";
		}
		return message;
	}

	public static TracedException addTrace(EObject sourceElement, Throwable cause) {
		if (cause instanceof TracedException tracedException && tracedException.sourceElement != null) {
			return tracedException;
		}
		return new TracedException(sourceElement, cause);
	}
}
