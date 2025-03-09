package tools.refinery.language.web;/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;

public class HealthCheckServlet extends HttpServlet {
	@Serial
	private static final long serialVersionUID = 5512947261931111041L;

	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		try (var writer = resp.getWriter()) {
			writer.println("{\"status\": \"up\"}");
			writer.flush();
		} catch (IOException e) {
			log.error("Failed to write response", e);
			if (!resp.isCommitted()) {
				resp.reset();
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}
}
