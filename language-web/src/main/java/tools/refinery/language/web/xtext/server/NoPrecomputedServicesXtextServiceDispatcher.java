package tools.refinery.language.web.xtext.server;

import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.model.PrecomputedServiceRegistry;

import com.google.inject.Singleton;

@Singleton
public class NoPrecomputedServicesXtextServiceDispatcher extends XtextServiceDispatcher {
	@Override
	protected void registerPreComputedServices(PrecomputedServiceRegistry registry) {
		// Do not register any precomputed services, because we will always send
		// requests for any pre-computation in the same websocket message as the
		// document update request.
	}
}
