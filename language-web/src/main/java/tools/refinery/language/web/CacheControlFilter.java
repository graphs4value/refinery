package tools.refinery.language.web;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

import org.eclipse.jetty.http.HttpHeader;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CacheControlFilter implements Filter {
	private static final Pattern CACHE_URI_PATTERN = Pattern.compile(".*\\.(css|gif|js|map|png|svg|woff2)");

	private static final Duration EXPIRY = Duration.ofDays(365);

	private static final String CACHE_CONTROL_CACHE_VALUE = "public, max-age: " + EXPIRY.toSeconds() + ", immutable";

	private static final String CACHE_CONTROL_NO_CACHE_VALUE = "no-cache, no-store, max-age: 0, must-revalidate";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to initialize.
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
			if (CACHE_URI_PATTERN.matcher(httpRequest.getRequestURI()).matches()) {
				httpResponse.setHeader(HttpHeader.CACHE_CONTROL.asString(), CACHE_CONTROL_CACHE_VALUE);
				httpResponse.setDateHeader(HttpHeader.EXPIRES.asString(),
						System.currentTimeMillis() + EXPIRY.toMillis());
			} else {
				httpResponse.setHeader(HttpHeader.CACHE_CONTROL.asString(), CACHE_CONTROL_NO_CACHE_VALUE);
				httpResponse.setDateHeader(HttpHeader.EXPIRES.asString(), 0);
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// Nothing to dispose.
	}
}
