/*
 * generated by Xtext 2.25.0
 */
package tools.refinery.language.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.SessionTrackingMode;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.config.BackendConfigServlet;
import tools.refinery.language.web.xtext.servlet.XtextWebSocketServlet;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Set;

public class ServerLauncher {
	public static final String DEFAULT_LISTEN_ADDRESS = "localhost";

	public static final int DEFAULT_LISTEN_PORT = 1312;

	public static final int DEFAULT_PUBLIC_PORT = 443;

	public static final int HTTP_DEFAULT_PORT = 80;

	public static final int HTTPS_DEFAULT_PORT = 443;

	public static final String ALLOWED_ORIGINS_SEPARATOR = ",";

	private static final Logger LOG = LoggerFactory.getLogger(ServerLauncher.class);

	private final Server server;

	public ServerLauncher(InetSocketAddress bindAddress, String[] allowedOrigins, String webSocketUrl) {
		server = VirtualThreadUtils.newServerWithVirtualThreadsThreadPool("jetty", bindAddress);
		var handler = new ServletContextHandler();
		addSessionHandler(handler);
		addProblemServlet(handler, allowedOrigins);
		addBackendConfigServlet(handler, webSocketUrl);
		var baseResource = getBaseResource();
		if (baseResource != null) {
			handler.setBaseResource(baseResource);
			handler.setWelcomeFiles(new String[]{"index.html"});
			addDefaultServlet(handler);
		}
		handler.addFilter(CacheControlFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		handler.addFilter(SecurityHeadersFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		server.setHandler(handler);
	}

	private void addSessionHandler(ServletContextHandler handler) {
		var sessionHandler = new SessionHandler();
		sessionHandler.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
		handler.setSessionHandler(sessionHandler);
	}

	private void addProblemServlet(ServletContextHandler handler, String[] allowedOrigins) {
		var problemServletHolder = new ServletHolder(ProblemWebSocketServlet.class);
		if (allowedOrigins == null) {
			LOG.warn("All WebSocket origins are allowed! This setting should not be used in production!");
		} else {
			var allowedOriginsString = String.join(XtextWebSocketServlet.ALLOWED_ORIGINS_SEPARATOR,
					allowedOrigins);
			problemServletHolder.setInitParameter(XtextWebSocketServlet.ALLOWED_ORIGINS_INIT_PARAM,
					allowedOriginsString);
		}
		handler.addServlet(problemServletHolder, "/xtext-service");
		JettyWebSocketServletContainerInitializer.configure(handler, null);
	}

	private void addBackendConfigServlet(ServletContextHandler handler, String webSocketUrl) {
		var backendConfigServletHolder = new ServletHolder(BackendConfigServlet.class);
		backendConfigServletHolder.setInitParameter(BackendConfigServlet.WEBSOCKET_URL_INIT_PARAM, webSocketUrl);
		handler.addServlet(backendConfigServletHolder, "/config.json");
	}

	private void addDefaultServlet(ServletContextHandler handler) {
		var defaultServletHolder = new ServletHolder(DefaultServlet.class);
		var isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		// Avoid file locking on Windows: https://stackoverflow.com/a/4985717
		// See also the related Jetty ticket:
		// https://github.com/eclipse/jetty.project/issues/2925
		defaultServletHolder.setInitParameter("useFileMappedBuffer", isWindows ? "false" : "true");
		handler.addServlet(defaultServletHolder, "/");
	}

	private Resource getBaseResource() {
		var factory = ResourceFactory.of(server);
		var baseResourceOverride = System.getenv("BASE_RESOURCE");
		if (baseResourceOverride != null) {
			// If a user override is provided, use it.
			return factory.newResource(baseResourceOverride);
		}
		var indexUrlInJar = ServerLauncher.class.getResource("/webapp/index.html");
		if (indexUrlInJar != null) {
			// If the app is packaged in the jar, serve it.
			URI webRootUri = null;
			try {
				webRootUri = URI.create(indexUrlInJar.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Jar has invalid base resource URI", e);
			}
			return factory.newResource(webRootUri);
		}
		// Look for unpacked production artifacts (convenience for running from IDE).
		var unpackedResourcePathComponents = new String[]{System.getProperty("user.dir"), "build", "webpack",
				"production"};
		var unpackedResourceDir = new File(String.join(File.separator, unpackedResourcePathComponents));
		if (unpackedResourceDir.isDirectory()) {
			return factory.newResource(unpackedResourceDir.toPath());
		}
		// Fall back to just serving a 404.
		return null;
	}

	public void start() throws Exception {
		server.start();
		LOG.info("Server started on {}", server.getURI());
		server.join();
	}

	public static void main(String[] args) {
		try {
			var bindAddress = getBindAddress();
			var allowedOrigins = getAllowedOrigins();
			var webSocketUrl = getWebSocketUrl();
			var serverLauncher = new ServerLauncher(bindAddress, allowedOrigins, webSocketUrl);
			serverLauncher.start();
		} catch (Exception exception) {
			LOG.error("Fatal server error", exception);
			System.exit(1);
		}
	}

	private static String getListenAddress() {
		var listenAddress = System.getenv("LISTEN_ADDRESS");
		if (listenAddress == null) {
			return DEFAULT_LISTEN_ADDRESS;
		}
		return listenAddress;
	}

	private static int getListenPort() {
		var portStr = System.getenv("LISTEN_PORT");
		if (portStr != null) {
			return Integer.parseInt(portStr);
		}
		return DEFAULT_LISTEN_PORT;
	}

	private static InetSocketAddress getBindAddress() {
		var listenAddress = getListenAddress();
		var listenPort = getListenPort();
		return new InetSocketAddress(listenAddress, listenPort);
	}

	private static String getPublicHost() {
		var publicHost = System.getenv("PUBLIC_HOST");
		if (publicHost != null) {
			return publicHost.toLowerCase();
		}
		return null;
	}

	private static int getPublicPort() {
		var portStr = System.getenv("PUBLIC_PORT");
		if (portStr != null) {
			return Integer.parseInt(portStr);
		}
		return DEFAULT_PUBLIC_PORT;
	}

	private static String[] getAllowedOrigins() {
		var allowedOrigins = System.getenv("ALLOWED_ORIGINS");
		if (allowedOrigins != null) {
			return allowedOrigins.split(ALLOWED_ORIGINS_SEPARATOR);
		}
		return getAllowedOriginsFromPublicHostAndPort();
	}

	// This method returns <code>null</code> to indicate that all origins are allowed.
	@SuppressWarnings("squid:S1168")
	private static String[] getAllowedOriginsFromPublicHostAndPort() {
		var publicHost = getPublicHost();
		if (publicHost == null) {
			return null;
		}
		int publicPort = getPublicPort();
		var scheme = publicPort == HTTPS_DEFAULT_PORT ? "https" : "http";
		var urlWithPort = String.format("%s://%s:%d", scheme, publicHost, publicPort);
		if (publicPort == HTTPS_DEFAULT_PORT || publicPort == HTTP_DEFAULT_PORT) {
			var urlWithoutPort = String.format("%s://%s", scheme, publicHost);
			return new String[]{urlWithPort, urlWithoutPort};
		}
		return new String[]{urlWithPort};
	}

	private static String getWebSocketUrl() {
		String host;
		int port;
		var publicHost = getPublicHost();
		if (publicHost == null) {
			host = getListenAddress();
			port = getListenPort();
		} else {
			host = publicHost;
			port = getPublicPort();
		}
		var scheme = port == HTTPS_DEFAULT_PORT ? "wss" : "ws";
		return String.format("%s://%s:%d/xtext-service", scheme, host, port);
	}
}
