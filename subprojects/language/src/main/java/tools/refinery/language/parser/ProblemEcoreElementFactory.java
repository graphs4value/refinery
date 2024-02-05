/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.parser;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
			ExplicitAssignmentTracker.install(problem);
		}
	}

	public static boolean hasExplicitlySetProblemKind(Problem problem) {
		return ExplicitAssignmentTracker.hasAdapter(problem);
	}

	private static class ExplicitAssignmentTracker extends AdapterImpl {
		@Override
		public boolean isAdapterForType(Object type) {
			return type == ExplicitAssignmentTracker.class;
		}

		public static boolean hasAdapter(Problem problem) {
			return EcoreUtil.getAdapter(problem.eAdapters(), ExplicitAssignmentTracker.class) != null;
		}

		public static void install(Problem problem) {
			if (hasAdapter(problem)) {
				throw new IllegalStateException("Duplicate explicit assignment of module kind");
			}
			problem.eAdapters().add(new ExplicitAssignmentTracker());
		}
	}
}
