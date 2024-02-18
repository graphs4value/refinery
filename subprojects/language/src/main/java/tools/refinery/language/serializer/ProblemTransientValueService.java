/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.serializer;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.parsetree.reconstr.impl.DefaultTransientValueService;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.utils.ProblemUtil;

public class ProblemTransientValueService extends DefaultTransientValueService {
	@Override
	public boolean isTransient(EObject owner, EStructuralFeature feature, int index) {
		if (owner instanceof Problem problem && feature == ProblemPackage.Literals.PROBLEM__KIND) {
			return problem.getName() == null && problem.getKind() == ProblemUtil.getDefaultModuleKind(problem) &&
					!problem.isExplicitKind();
		}
		return super.isTransient(owner, feature, index);
	}
}
