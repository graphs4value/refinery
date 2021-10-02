package org.eclipse.viatra.solver.language.web;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CacheControlFilter implements Filter {

	private static final String CACHE_CONTROL_HEADER = "Cache-Control";

	private static final String EXPIRES_HEADER = "Expires";

	private static final Pattern CACHE_URI_PATTERN = Pattern.compile(".*\\.(css|gif|js|map|png|svg|woff2)");

	private static final long EXPIRY = 31536000;

	private static final String CACHE_CONTROL_CACHE_VALUE = "public, max-age: " + EXPIRY + ", immutable";

	private static final String CACHE_CONTROL_NO_CACHE_VALUE = "no-cache, no-store, max-age: 0, must-revalidate";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to initialize.
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			var httpRequest = (HttpServletRequest) request;
			var httpResponse = (HttpServletResponse) response;
			if (CACHE_URI_PATTERN.matcher(httpRequest.getRequestURI()).matches()) {
				httpResponse.setHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_CACHE_VALUE);
				httpResponse.setDateHeader(EXPIRES_HEADER, System.currentTimeMillis() + EXPIRY * 1000L);
			} else {
				httpResponse.setHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_NO_CACHE_VALUE);
				httpResponse.setDateHeader(EXPIRES_HEADER, 0);
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// Nothing to dispose.
	}
}
