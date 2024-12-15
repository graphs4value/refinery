/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.hover;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.web.server.hover.HoverResult;
import org.eclipse.xtext.web.server.hover.HoverService;
import tools.refinery.language.ide.contentassist.ProblemProposalUtils;
import tools.refinery.language.model.problem.Variable;
import tools.refinery.language.naming.NamingUtil;

@Singleton
public class ProblemHoverService extends HoverService {
	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private ProblemProposalUtils proposalUtils;

	@Override
	protected HoverResult createHover(Object element, String stateId, CancelIndicator cancelIndicator) {
		operationCanceledManager.checkCanceled(cancelIndicator);
		var eObject = switch (element) {
			case EObject theEObject -> theEObject;
			case IEObjectDescription description -> description.getEObjectOrProxy();
			case null, default -> null;
		};
		if (eObject == null) {
			return new HoverResult(stateId);
		}
		var title = getTitle(eObject);
		var content = proposalUtils.getDocumentation(eObject);
		return new HoverResult(stateId, title, content);
	}

	private String getTitle(EObject eObject) {
		var name = getName(eObject);
		if (name == null) {
			return null;
		}
		var builder = new StringBuilder();
		builder.append("<span class=\"cm-completionIcon");
		for (var kind : proposalUtils.getKind(eObject)) {
			builder.append(" cm-completionIcon-").append(kind);
		}
		builder.append("\"></span><span class=\"cm-completionLabel\"><span>")
				.append(name)
				.append("</span></span>");
		var description = proposalUtils.getDescription(eObject);
		if (description != null) {
			builder.append("<span class=\"cm-completionDetail\">");
			if (!description.startsWith("/")) {
				builder.append("&nbsp;");
			}
			builder.append(description)
					.append("</span>");
		}
		return builder.toString();
	}

	private String getName(EObject eObject) {
		if (eObject instanceof Variable variable) {
			// Variables are never in the global scope.
			return variable.getName();
		}
		var qualifiedName = NamingUtil.stripRootPrefix(qualifiedNameProvider.getFullyQualifiedName(eObject));
		if (qualifiedName == null) {
			return null;
		}
		return qualifiedNameConverter.toString(qualifiedName);
	}
}
