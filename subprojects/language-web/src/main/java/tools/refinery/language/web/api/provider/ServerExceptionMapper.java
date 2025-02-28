/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.provider;

import com.google.gson.JsonParseException;
import com.google.gson.stream.MalformedJsonException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.generator.GeneratorTimeoutException;
import tools.refinery.generator.InvalidProblemException;
import tools.refinery.generator.UnsatisfiableProblemException;
import tools.refinery.language.web.api.dto.RefineryResponse;

import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class ServerExceptionMapper {
	private static final Logger LOG = LoggerFactory.getLogger(ServerExceptionMapper.class);

	/**
	 * Regular expression for extracting the JSON path from a Gson error message.
	 * <p>
	 * For more information, see the
	 * <a href="https://github.com/google/gson/blob/e5dce841f73382cb7acdfe32250767ddb2c86b49/gson/src/main/java/com/google/gson/stream/JsonReader.java#L1645">
	 * Gson source code</a>.
	 * </p>
	 */
	private static final Pattern JSON_ERROR_REGEX = Pattern.compile(" at line \\d+ column \\d+ path (.+)$",
			Pattern.MULTILINE);

	@Inject
	private OperationCanceledManager operationCanceledManager;

	public Response toResponse(Throwable exception) {
		if (operationCanceledManager.isOperationCanceledException(exception)) {
			LOG.debug("Operation canceled", exception);
			return toResponse(RefineryResponse.Cancelled.of());
		}
		return switch (exception) {
			case WebApplicationException webApplicationException -> {
				var response = webApplicationException.getResponse();
				int status = response.getStatus();
				yield switch (response.getStatusInfo().getFamily()) {
					case CLIENT_ERROR -> {
						LOG.debug("Client error", webApplicationException);
						yield Response.status(status)
								.type(MediaType.APPLICATION_JSON_TYPE)
								.entity(new RefineryResponse.RequestError(webApplicationException.getMessage()))
								.build();
					}
					case SERVER_ERROR -> {
						LOG.error("Server error", webApplicationException);
						yield Response.status(status)
								.type(MediaType.APPLICATION_JSON_TYPE)
								.entity(new RefineryResponse.ServerError(webApplicationException.getMessage()))
								.build();
					}
					default -> response;
				};
			}
			case MalformedJsonException malformedJsonException -> {
				LOG.debug("JSON parser exception", malformedJsonException);
				yield toResponse(translateJsonException(malformedJsonException));
			}
			case JsonParseException jsonParseException -> {
				LOG.debug("JSON parser exception", jsonParseException);
				var cause = jsonParseException.getCause();
				var toTranslate = cause == null ? jsonParseException : cause;
				yield toResponse(translateJsonException(toTranslate));
			}
			case InvalidProblemException invalidProblemException -> {
				LOG.debug("Invalid problem", invalidProblemException);
				yield toResponse(RefineryResponse.InvalidProblem.ofValidationErrorsException(
						invalidProblemException));
			}
			case UnsatisfiableProblemException unsatisfiableProblemException -> {
				LOG.debug("Unsatisfiable problem", unsatisfiableProblemException);
				yield toResponse(new RefineryResponse.Unsatisfiable(unsatisfiableProblemException.getMessage()));
			}
			case GeneratorTimeoutException generatorTimeoutException -> {
				LOG.debug("Generator timed out", generatorTimeoutException);
				yield toResponse(RefineryResponse.Timeout.of());
			}
			case ConstraintViolationException constraintViolationException ->
					toResponse(translateConstraintViolationException(constraintViolationException));
			case null, default -> {
				LOG.error("Unexpected exception", exception);
				yield toResponse(RefineryResponse.ServerError.of());
			}
		};
	}

	private Response toResponse(RefineryResponse refineryResponse) {
		return Response.status(refineryResponse.getStatus())
				.type(MediaType.APPLICATION_JSON_TYPE)
				.entity(refineryResponse)
				.build();
	}

	private RefineryResponse.RequestError translateJsonException(Throwable t) {
		var message = t.getMessage();
		if (message == null) {
			return RefineryResponse.RequestError.of();
		}
		var matcher = JSON_ERROR_REGEX.matcher(message);
		if (!matcher.find()) {
			return new RefineryResponse.RequestError("Invalid JSON: " + message);
		}
		var path = matcher.group(1);
		return new RefineryResponse.RequestError("Invalid JSON", List.of(
				new RefineryResponse.RequestError.Detail(path, message)
		));
	}

	private RefineryResponse.RequestError translateConstraintViolationException(ConstraintViolationException e) {
		LOG.debug("Invalid request payload", e);
		var details = e.getConstraintViolations().stream()
				.map(violation -> {
					var path = violation.getPropertyPath();
					var pathBuilder = new StringBuilder("$");
					for (var node : path) {
						var kind = node.getKind();
						// The path starts with the called method of the resource bean and its argument index,
						// but the client doesn't need this information, so we just skip it.
						if (kind != ElementKind.METHOD && kind != ElementKind.PARAMETER) {
							var nodeString = node.toString();
							if (!nodeString.isEmpty()) {
								// Applying a validation annotation directly to a class might result in an empty
								// path segment.
								pathBuilder.append(".").append(nodeString);
							}
						}
					}
					return new RefineryResponse.RequestError.Detail(pathBuilder.toString(), violation.getMessage());
				})
				.toList();
		return new RefineryResponse.RequestError("Invalid request payload", details);
	}
}
