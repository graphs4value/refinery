/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class SecurityHeadersFilter extends HttpFilter {
	private static final Logger LOG = LoggerFactory.getLogger(SecurityHeadersFilter.class);

	public static final String ALLOWED_ORIGINS_SEPARATOR = ",";

	public static final String ALLOWED_ORIGINS_INIT_PARAM =
			"tools.refinery.language.web.SecurityHeadersFilter.allowedOrigins";

	private transient Set<String> allowedOrigins = null;

	@Override
	public void init(FilterConfig config) {
		var allowedOriginsStr = config.getInitParameter(ALLOWED_ORIGINS_INIT_PARAM);
		if (allowedOriginsStr == null) {
			LOG.warn("All HTTP request origins are allowed! This setting should not be used in production!");
		} else {
			allowedOrigins = Set.of(allowedOriginsStr.split(ALLOWED_ORIGINS_SEPARATOR));
			LOG.info("Allowed origins: {}", allowedOrigins);
		}
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		response.setHeader("Content-Security-Policy", "default-src 'none'; " +
				"script-src 'self' 'unsafe-eval' 'wasm-unsafe-eval'; " +
				// CodeMirror needs inline styles, see e.g.,
				// https://discuss.codemirror.net/t/inline-styles-and-content-security-policy/1311/2
				"style-src 'self' 'unsafe-inline'; " +
				// Use 'data:' for displaying inline SVG backgrounds and blob for rendering SVG.
				"img-src 'self' data: blob:; " +
				"font-src 'self'; " +
				// Fetch data:application/octet-stream;base64 URIs to unpack compressed URL fragments.
				"connect-src 'self' data:; " +
				"manifest-src 'self'; " +
				"worker-src 'self' blob:;");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "strict-origin");
		// Enable cross-origin isolation, https://web.dev/cross-origin-isolation-guide/
		response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
		response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
		// We do not expose any sensitive data over HTTP, so <code>cross-origin</code> is safe here.
		response.setHeader("Cross-Origin-Resource-Policy", "cross-origin");

		if (!"CONNECT".equals(request.getMethod())) {
			// Cross-origin request for WebSockets are handled by {@code XtextWebSocketServlet}.
			var origin = request.getHeader("Origin");
			if (origin != null && allowedOrigins != null) {
				if (allowedOrigins.contains(origin)) {
					response.setHeader("Access-Control-Allow-Origin", origin);
					response.setHeader("Access-Control-Allow-Headers", "Content-Type");
					response.setHeader("Vary", "Origin");
				} else {
					// Reject cross-origin requests from invalid origins.
					response.sendError(Response.Status.UNAUTHORIZED.getStatusCode());
					return;
				}
			}
		}

		chain.doFilter(request, response);
	}
}
