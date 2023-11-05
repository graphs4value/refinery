/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * generated by Xtext 2.25.0
 */
package tools.refinery.language.validation;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.Check;

import com.google.inject.Inject;

import tools.refinery.language.model.problem.*;
import tools.refinery.language.resource.ReferenceCounter;
import tools.refinery.language.utils.ProblemUtil;

/**
 * This class contains custom validation rules.
 * <p>
 * See
 * <a href="https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation">...</a>
 */
public class ProblemValidator extends AbstractProblemValidator {
	private static final String ISSUE_PREFIX = "tools.refinery.language.validation.ProblemValidator.";

	public static final String SINGLETON_VARIABLE_ISSUE = ISSUE_PREFIX + "SINGLETON_VARIABLE";

	public static final String NON_INDIVIDUAL_NODE_ISSUE = ISSUE_PREFIX + "NON_INDIVIDUAL_NODE";

	@Inject
	private ReferenceCounter referenceCounter;

	@Check
	public void checkUniqueVariable(VariableOrNodeExpr expr) {
		var variableOrNode = expr.getVariableOrNode();
		if (variableOrNode instanceof Variable variable && ProblemUtil.isImplicitVariable(variable)
				&& !ProblemUtil.isSingletonVariable(variable)) {
			var problem = EcoreUtil2.getContainerOfType(variable, Problem.class);
			if (problem != null && referenceCounter.countReferences(problem, variable) <= 1) {
				var name = variable.getName();
				var message = ("Variable '%s' has only a single reference. " +
						"Add another reference or mark is as a singleton variable: '_%s'").formatted(name, name);
				warning(message, expr, ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE,
						INSIGNIFICANT_INDEX, SINGLETON_VARIABLE_ISSUE);
			}
		}
	}

	@Check
	public void checkNonUniqueNode(VariableOrNodeExpr expr) {
		var variableOrNode = expr.getVariableOrNode();
		if (variableOrNode instanceof Node node && !ProblemUtil.isIndividualNode(node)) {
			var name = node.getName();
			var message = ("Only individuals can be referenced in predicates. " +
					"Mark '%s' as individual with the declaration 'indiv %s.'").formatted(name, name);
			error(message, expr, ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__VARIABLE_OR_NODE,
					INSIGNIFICANT_INDEX, NON_INDIVIDUAL_NODE_ISSUE);
		}
	}
}
