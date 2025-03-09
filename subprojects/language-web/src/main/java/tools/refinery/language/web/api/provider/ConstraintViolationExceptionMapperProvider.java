/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.provider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * We must inject this in addition to {@link ServerExceptionMapperProvider} to override the default exception mapper
 * for {@link ConstraintViolationException} injected by Jersey, as {@link ServerExceptionMapperProvider} doesn't have
 * a specific enough type.
 */
@Singleton
@Provider
public class ConstraintViolationExceptionMapperProvider implements ExceptionMapper<ConstraintViolationException> {
	private final ServerExceptionMapper serverExceptionMapper;

	@Inject
	public ConstraintViolationExceptionMapperProvider(ServerExceptionMapper serverExceptionMapper) {
		this.serverExceptionMapper = serverExceptionMapper;
	}

	@Override
	public Response toResponse(ConstraintViolationException exception) {
		return serverExceptionMapper.toResponse(exception);
	}
}
