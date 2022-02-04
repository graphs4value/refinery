package tools.refinery.language.web.xtext.server.push;

import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.model.XtextWebDocument;

import com.google.inject.Singleton;

import tools.refinery.language.web.xtext.server.SubscribingServiceContext;

@Singleton
public class PushServiceDispatcher extends XtextServiceDispatcher {

	@Override
	protected XtextWebDocument getFullTextDocument(String fullText, String resourceId, IServiceContext context) {
		var document = super.getFullTextDocument(fullText, resourceId, context);
		if (document instanceof PushWebDocument pushWebDocument
				&& context instanceof SubscribingServiceContext subscribingContext) {
			pushWebDocument.addPrecomputationListener(subscribingContext.subscriber());
		}
		return document;
	}
}
