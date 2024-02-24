/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class SecurityHeadersFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (response instanceof HttpServletResponse httpResponse) {
			httpResponse.setHeader("Content-Security-Policy", "default-src 'none'; " +
					"script-src 'self' 'wasm-unsafe-eval'; " +
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
			httpResponse.setHeader("X-Content-Type-Options", "nosniff");
			httpResponse.setHeader("X-Frame-Options", "DENY");
			httpResponse.setHeader("Referrer-Policy", "strict-origin");
			// Enable cross-origin isolation, https://web.dev/cross-origin-isolation-guide/
			httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
			httpResponse.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
			// We do not expose any sensitive data over HTTP, so <code>cross-origin</code> is safe here.
			httpResponse.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
		}
		chain.doFilter(request, response);
	}
}
