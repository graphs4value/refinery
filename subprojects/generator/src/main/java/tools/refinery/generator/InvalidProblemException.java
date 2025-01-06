/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.validation.Issue;

import java.util.List;

public class InvalidProblemException extends IllegalArgumentException {
	private final transient URI resourceUri;

	private final transient List<Issue> errors;

	public InvalidProblemException(URI resourceUri, List<Issue> errors) {
		this(resourceUri, errors, null);
	}

	public InvalidProblemException(URI resourceUri, List<Issue> errors, Throwable cause) {
		super(cause == null ? null : cause.getMessage(), cause);
		this.resourceUri = resourceUri;
		this.errors = errors;
	}

	public URI getResourceUri() {
		return resourceUri;
	}

	public List<Issue> getErrors() {
		return errors;
	}

	public String getShortMessage() {
		var detailMessage = super.getMessage();
		return detailMessage == null ? "Validation errors detected" : detailMessage;
	}

	@Override
	public String getMessage() {
		var detailMessage = super.getMessage();
		if (detailMessage != null) {
			return detailMessage;
		}
		var builder = new StringBuilder();
		builder.append("Errors ");
		if (resourceUri != null) {
			builder.append("in resource ");
			builder.append(resourceUri);
		}
		builder.append(": ");
		var iterator = errors.iterator();
		if (!iterator.hasNext()) {
			return builder.toString();
		}
		builder.append(iterator.next());
		while (iterator.hasNext()) {
			builder.append(",\n");
			builder.append(iterator.next());
		}
		return builder.toString();
	}
}
