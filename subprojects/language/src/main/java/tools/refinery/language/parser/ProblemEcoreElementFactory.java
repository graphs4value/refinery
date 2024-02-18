/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.parser;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.parser.DefaultEcoreElementFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;

public class ProblemEcoreElementFactory extends DefaultEcoreElementFactory {
	@Override
	public void set(
			EObject object, String feature, Object value, String ruleName, INode node) throws ValueConverterException {
		super.set(object, feature, value, ruleName, node);
		if (object instanceof Problem problem && ProblemPackage.Literals.PROBLEM__KIND.getName().equals(feature)) {
			problem.setExplicitKind(true);
		}
	}
}
