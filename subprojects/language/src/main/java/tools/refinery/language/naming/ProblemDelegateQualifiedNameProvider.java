/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.naming;

import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.model.problem.Problem;

public class ProblemDelegateQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	protected QualifiedName qualifiedName(Problem ele) {
		var qualifiedName = computeFullyQualifiedNameFromNameAttribute(ele);
		// Strip the root prefix even if explicitly provided.
		return NamingUtil.stripRootPrefix(qualifiedName);
	}
}
