/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
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
		return new PushWebDocument(resourceId,
				serviceContext.getSession().get(DocumentSynchronizer.class, () -> this.synchronizerProvider.get()));
	}
}
