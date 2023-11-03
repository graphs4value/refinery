/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.validation.Issue;

import java.util.List;

public class ValidationErrorsException extends IllegalArgumentException {
	private final transient URI resourceUri;

	private final String resourceUriString;

	private final transient List<Issue> errors;

	private final List<String> errorStrings;

	public ValidationErrorsException(URI resourceUri, List<Issue> errors) {
		this.resourceUri = resourceUri;
		resourceUriString = resourceUri.toString();
		this.errors = errors;
		errorStrings = errors.stream()
				.map(Issue::toString)
				.toList();
	}

	public URI getResourceUri() {
		return resourceUri;
	}

	public String getResourceUriString() {
		return resourceUriString;
	}

	public List<Issue> getErrors() {
		return errors;
	}

	public List<String> getErrorStrings() {
		return errorStrings;
	}

	@Override
	public String getMessage() {
		var builder = new StringBuilder();
		builder.append("Validation errors in resource ");
		builder.append(resourceUriString);
		builder.append(": ");
		var iterator = errorStrings.iterator();
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
