/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.push;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.model.DocumentSynchronizer;
import org.eclipse.xtext.web.server.model.IWebDocumentProvider;
import org.eclipse.xtext.web.server.model.XtextWebDocument;

import java.util.Optional;

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
		var session = serviceContext.getSession().get(DocumentSynchronizer.class,
				() -> this.synchronizerProvider.get());
		boolean concretize = getConcretize(serviceContext).orElse(false);
		return new PushWebDocument(resourceId, session, concretize);
	}

	public static Optional<Boolean> getConcretize(IServiceContext serviceContext) {
		var value = serviceContext.getParameter("concretize");
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of("true".equals(value));
	}
}
