/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.config;

import com.google.gson.Gson;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;

import java.io.IOException;

public class BackendConfigServlet extends HttpServlet {
	public static final String WEBSOCKET_URL_INIT_PARAM = "tools.refinery.language.web.config.BackendConfigServlet" +
			".webSocketUrl";

	private String serializedConfig;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		var webSocketUrl = config.getInitParameter(WEBSOCKET_URL_INIT_PARAM);
		var backendConfig = new BackendConfig(webSocketUrl);
		var gson = new Gson();
		serializedConfig = gson.toJson(backendConfig);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setStatus(HttpStatus.OK_200);
		resp.setContentType("application/json");
		var writer = resp.getWriter();
		writer.write(serializedConfig);
		writer.flush();
	}
}
