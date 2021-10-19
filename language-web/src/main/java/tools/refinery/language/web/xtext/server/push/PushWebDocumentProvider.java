package tools.refinery.language.web.xtext.server.push;

import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.model.DocumentSynchronizer;
import org.eclipse.xtext.web.server.model.IWebDocumentProvider;
import org.eclipse.xtext.web.server.model.XtextWebDocument;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Based on
 * {@link org.eclipse.xtext.web.server.model.IWebDocumentProvider.DefaultImpl}.
 * 
 * @author Kristóf Marussy
 */
@Singleton
public class PushWebDocumentProvider implements IWebDocumentProvider {
	@Inject
	private Provider<DocumentSynchronizer> synchronizerProvider;

	@Override
	public XtextWebDocument get(String resourceId, IServiceContext serviceContext) {
		if (resourceId == null) {
			return new XtextWebDocument(resourceId, synchronizerProvider.get());
		} else {
			// We only need to send push messages if a resourceId is specified.
			return new PushWebDocument(resourceId,
					serviceContext.getSession().get(DocumentSynchronizer.class, () -> this.synchronizerProvider.get()));
		}
	}
}
