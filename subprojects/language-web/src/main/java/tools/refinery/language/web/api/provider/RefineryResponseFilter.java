/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.provider;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import tools.refinery.language.web.api.dto.RefineryResponse;

import java.io.IOException;

@Provider
public class RefineryResponseFilter implements ContainerResponseFilter {
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		if (responseContext.getStatus() == Response.Status.OK.getStatusCode() &&
				responseContext.getEntity() instanceof RefineryResponse refineryResponse) {
			responseContext.setStatus(refineryResponse.getStatus().getStatusCode());
		}
	}
}
