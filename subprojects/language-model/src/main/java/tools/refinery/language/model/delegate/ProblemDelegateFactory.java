/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.delegate;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate;
import tools.refinery.language.model.problem.ProblemPackage;

public class ProblemDelegateFactory implements SettingDelegate.Factory {
	public static final String URI = "https://refinery.tools/emf/2024/ProblemDelegate";

	@Override
	public SettingDelegate createSettingDelegate(EStructuralFeature eStructuralFeature) {
		if (ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE.equals(eStructuralFeature)) {
			return new ElementDelegate(ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE);
		}
		if (ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__RELATION.equals(eStructuralFeature)) {
			return new ElementDelegate(ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__RELATION);
		}
		throw new IllegalArgumentException("Unknown EStructuralFeature: " + eStructuralFeature);
	}
}
