package tools.refinery.language.web;

import org.eclipse.xtext.util.DisposableRegistry;

import jakarta.servlet.ServletException;
import tools.refinery.language.web.xtext.XtextWebSocketServlet;

public class ProblemWebSocketServlet extends XtextWebSocketServlet {

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
