/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.DefaultLocationInFileProvider;
import org.eclipse.xtext.util.ITextRegion;
import tools.refinery.language.model.problem.ImplicitVariable;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.utils.ProblemUtil;

public class ProblemLocationInFileProvider extends DefaultLocationInFileProvider {
	@Override
	protected ITextRegion doGetTextRegion(EObject obj, RegionDescription query) {
		if (obj instanceof Node node) {
			return getNodeTextRegion(node, query);
		}
		if (obj instanceof ImplicitVariable) {
			return ITextRegion.EMPTY_REGION;
		}
		return super.doGetTextRegion(obj, query);
	}

	protected ITextRegion getNodeTextRegion(Node node, RegionDescription query) {
		if (ProblemUtil.isDeclaredNode(node)) {
			return super.doGetTextRegion(node, query);
		}
		if (ProblemUtil.isMultiNode(node)) {
			EObject container = node.eContainer();
			return doGetTextRegion(container, query);
		}
		return ITextRegion.EMPTY_REGION;
	}
}
