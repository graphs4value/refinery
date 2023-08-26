/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web;

import org.eclipse.xtext.util.DisposableRegistry;

import jakarta.servlet.ServletException;
import tools.refinery.language.web.xtext.servlet.XtextWebSocketServlet;

import java.io.Serial;

public class ProblemWebSocketServlet extends XtextWebSocketServlet {
	@Serial
	private static final long serialVersionUID = -7040955470384797008L;

	private transient DisposableRegistry disposableRegistry;

	@Override
	public void init() throws ServletException {
		super.init();
		var injector = new ProblemWebSetup().createInjectorAndDoEMFRegistration();
		this.disposableRegistry = injector.getInstance(DisposableRegistry.class);
	}

	@Override
	public void destroy() {
		if (disposableRegistry != null) {
			disposableRegistry.dispose();
			disposableRegistry = null;
		}
		super.destroy();
	}
}
