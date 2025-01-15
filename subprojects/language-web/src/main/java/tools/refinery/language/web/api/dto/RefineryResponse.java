/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import jakarta.ws.rs.core.Response;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import tools.refinery.generator.InvalidProblemException;

import java.util.List;

public sealed interface RefineryResponse {
	Response.Status getStatus();

	record Timeout(String message) implements RefineryResponse {
		private static final String DEFAULT_MESSAGE = "Request timed out";

		@Override
		public Response.Status getStatus() {
			return Response.Status.REQUEST_TIMEOUT;
		}

		public static Timeout of() {
			return new Timeout(DEFAULT_MESSAGE);
		}
	}

	record Cancelled(String message) implements RefineryResponse {
		@Override
		public Response.Status getStatus() {
			return Response.Status.OK;
		}

		public static Cancelled of() {
			return new Cancelled("Operation cancelled");
		}
	}

	record RequestError(String message, List<Detail> details) implements RefineryResponse {
		private static final String DEFAULT_MESSAGE = "Bad request";

		public RequestError(String message) {
			this(message, List.of());
		}

		@Override
		public Response.Status getStatus() {
			return Response.Status.BAD_REQUEST;
		}

		public static RequestError of() {
			return new RequestError(DEFAULT_MESSAGE);
		}

		public record Detail(String propertyPath, String message) {
		}
	}

	record ServerError(String message) implements RefineryResponse {
		private static final String DEFAULT_MESSAGE = "Internal error";

		@Override
		public Response.Status getStatus() {
			return Response.Status.INTERNAL_SERVER_ERROR;
		}

		public static ServerError of() {
			return new ServerError(DEFAULT_MESSAGE);
		}
	}

	record InvalidProblem(String message, List<ValidationResult.Issue> issues) implements RefineryResponse {
		@Override
		public Response.Status getStatus() {
			return Response.Status.BAD_REQUEST;
		}

		public static InvalidProblem ofValidationErrorsException(InvalidProblemException e) {
			var issues = translateIssues(e);
			return new InvalidProblem(e.getShortMessage(), issues);
		}

		private static List<ValidationResult.Issue> translateIssues(InvalidProblemException e) {
			var resourceUri = e.getResourceUri();
			return e.getErrors().stream()
					.map(issue -> {
						var severity = translateSeverity(issue.getSeverity());
						var uri = issue.getUriToProblem();
						// Syntax errors have no {@code uriToProblem}.
						if (uri == null || uri.trimFragment().equals(resourceUri)) {
							return new ValidationResult.Issue(issue.getMessage(), severity, issue.getLineNumber(),
									issue.getColumn(), issue.getOffset(), issue.getLength());
						}
						var message = "%s (%s line %d column %d)".formatted(issue.getMessage(), uri.trimFragment(),
								issue.getLineNumber(), issue.getColumn());
						return new ValidationResult.Issue(message, severity, 0, 0, 0, 0);
					})
					.toList();
		}

		private static String translateSeverity(Severity severity) {
			return switch (severity) {
				case Severity.WARNING -> "warning";
				case Severity.ERROR -> "message";
				case Severity.INFO -> "info";
				case null, default -> "ignore";
			};
		}
	}

	record Unsatisfiable(String message) implements RefineryResponse {
		@Override
		public Response.Status getStatus() {
			return Response.Status.OK;
		}
	}

	record Success(Object value) implements RefineryResponse {
		@Override
		public Response.Status getStatus() {
			return Response.Status.OK;
		}
	}

	record Status(Object value) implements RefineryResponse {
		@Override
		public Response.Status getStatus() {
			return Response.Status.ACCEPTED;
		}
	}
}
