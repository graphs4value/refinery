/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource;

import com.google.common.base.Predicate;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.findReferences.ReferenceFinder;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.utils.ProblemUtil;

public class ProblemReferenceFinder extends ReferenceFinder {
	@Override
	// {@link com.google.common.base.Predicate} required by Xtext API.
	@SuppressWarnings("squid:S4738")
	protected boolean doProcess(EObject sourceCandidate, Predicate<URI> targetURIs) {
		if (sourceCandidate instanceof Relation relation) {
			// References within derived predicates would show up as references within their corresponding parent
			// element, so we skip them.
			return !ProblemUtil.isComputedValuePredicate(relation) &&
					!ProblemUtil.isInvalidMultiplicityConstraint(relation);
		}
		return super.doProcess(sourceCandidate, targetURIs);
	}
}
