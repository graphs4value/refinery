package tools.refinery.language.web.xtext;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

public abstract class XtextWebSocketServlet extends JettyWebSocketServlet implements JettyWebSocketCreator {

	private static final long serialVersionUID = -3772740838165122685L;

	public static final String ALLOWED_ORIGINS_SEPARATOR = ";";

	public static final String ALLOWED_ORIGINS_INIT_PARAM = "tools.refinery.language.web.xtext.XtextWebSocketServlet.allowedOrigin";

	/**
	 * Maximum message size should be large enough to upload a full model file.
	 */
	private static final long MAX_FRAME_SIZE = 4L * 1024L * 1024L;

	private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);

	private transient Logger log = LoggerFactory.getLogger(getClass());

	private transient Set<String> allowedOrigins = null;

	@Override
	public void init(ServletConfig config) throws ServletException {
		var allowedOriginsStr = config.getInitParameter(ALLOWED_ORIGINS_INIT_PARAM);
		if (allowedOriginsStr == null) {
			log.warn("All WebSocket origins are allowed! This setting should not be used in production!");
		} else {
			allowedOrigins = Set.of(allowedOriginsStr.split(ALLOWED_ORIGINS_SEPARATOR));
			log.info("Allowed origins: {}", allowedOrigins);
		}
		super.init(config);
	}

	@Override
	protected void configure(JettyWebSocketServletFactory factory) {
		factory.setMaxFrameSize(MAX_FRAME_SIZE);
		factory.setIdleTimeout(IDLE_TIMEOUT);
		factory.addMapping("/", this);
	}

	@Override
	public Object createWebSocket(JettyServerUpgradeRequest req, JettyServerUpgradeResponse resp) {
		if (allowedOrigins != null) {
			var origin = req.getOrigin();
			if (origin == null || !allowedOrigins.contains(origin.toLowerCase())) {
				log.error("Connection from {} from forbidden origin {}", req.getRemoteSocketAddress(), origin);
				try {
					resp.sendForbidden("Origin not allowed");
				} catch (IOException e) {
					log.error("Cannot send forbidden origin error", e);
				}
				return null;
			}
		}
		var session = new SimpleSession();
		return new XtextWebSocket(session, IResourceServiceProvider.Registry.INSTANCE);
	}
}
