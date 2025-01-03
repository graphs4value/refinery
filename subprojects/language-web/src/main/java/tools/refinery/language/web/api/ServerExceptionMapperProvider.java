/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Wraps {@link ServerExceptionMapper} for Jakarta HK2 dependency injection.
 */
@Singleton
@Provider
public class ServerExceptionMapperProvider implements ExceptionMapper<Throwable> {
	private final ServerExceptionMapper serverExceptionMapper;

	@Inject
	public ServerExceptionMapperProvider(ServerExceptionMapper serverExceptionMapper) {
		this.serverExceptionMapper = serverExceptionMapper;
	}

	@Override
	public Response toResponse(Throwable exception) {
		return serverExceptionMapper.toResponse(exception);
	}
}
