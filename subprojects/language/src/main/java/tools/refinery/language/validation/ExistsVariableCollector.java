/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.utils.ProblemUtil;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class ExistsVariableCollector {
	private static final String EXISTS_VARIABLES =
			"tools.refinery.language.validation.ExistsVariableCollector.EXISTS_VARIABLES";

	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	public boolean missingExistsConstraint(Variable variable) {
		if (variable instanceof Parameter) {
			return false;
		}
		if (ProblemUtil.isImplicitVariable(variable)) {
			var negation = EcoreUtil2.getContainerOfType(variable, NegationExpr.class);
			if (negation != null) {
				// Negations are implicitly quantified.
				return false;
			}
		}
		var conjunction = EcoreUtil2.getContainerOfType(variable, Conjunction.class);
		if (conjunction == null) {
			return true;
		}
		var variables = getExistsVariables(conjunction);
		return !variables.contains(variable);
	}

	protected Set<Variable> getExistsVariables(Conjunction conjunction) {
		var resource = conjunction.eResource();
		if (resource == null) {
			return doGetExistsVariables(conjunction);
		}
		return cache.get(Tuples.create(conjunction, EXISTS_VARIABLES), resource,
				() -> doGetExistsVariables(conjunction));
	}

	protected Set<Variable> doGetExistsVariables(Conjunction conjunction) {
		var builtinSymbols = importAdapterProvider.getBuiltinSymbols(conjunction);
		var existsRelation = builtinSymbols.exists();
		var set = new HashSet<Variable>();
		for (var atom : EcoreUtil2.getAllContentsOfType(conjunction, Atom.class)) {
			if (existsRelation.equals(atom.getRelation())) {
				for (var argument : atom.getArguments()) {
					if (argument instanceof VariableOrNodeExpr variableOrNodeExpr &&
							variableOrNodeExpr.getVariableOrNode() instanceof Variable variable) {
						set.add(variable);
					}
				}
			}
		}
		return set;
	}
}
