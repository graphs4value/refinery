/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.eclipse.jetty.ee10.websocket.server.*;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.time.Duration;
import java.util.Set;

public abstract class XtextWebSocketServlet extends JettyWebSocketServlet implements JettyWebSocketCreator {
	@Serial
	private static final long serialVersionUID = -3772740838165122685L;

	public static final String ALLOWED_ORIGINS_SEPARATOR = ",";

	public static final String ALLOWED_ORIGINS_INIT_PARAM =
			"tools.refinery.language.web.xtext.XtextWebSocketServlet.allowedOrigin";

	public static final String XTEXT_SUBPROTOCOL_V1 = "tools.refinery.language.web.xtext.v1";

	/**
	 * Maximum message size should be large enough to upload a full model file.
	 */
	private static final long MAX_FRAME_SIZE = 4L * 1024L * 1024L;

	private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(30);

	private final transient Logger log = LoggerFactory.getLogger(getClass());

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
		if (req.getSubProtocols().contains(XTEXT_SUBPROTOCOL_V1)) {
			resp.setAcceptedSubProtocol(XTEXT_SUBPROTOCOL_V1);
		} else {
			log.error("None of the subprotocols {} offered by {} are supported", req.getSubProtocols(),
					req.getRemoteSocketAddress());
			resp.setAcceptedSubProtocol(null);
		}
		var session = new SimpleSession();
		return new XtextWebSocket(session, IResourceServiceProvider.Registry.INSTANCE);
	}
}
