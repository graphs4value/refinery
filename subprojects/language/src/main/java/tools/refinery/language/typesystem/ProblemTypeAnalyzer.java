/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.model.problem.Expr;
import tools.refinery.language.model.problem.Problem;

@Singleton
public class ProblemTypeAnalyzer {
	private static final String CACHE_KEY = "tools.refinery.language.typesystem.ProblemTypeAnalyzer.CACHE_KEY";

	@Inject
	private IResourceScopeCache resourceScopeCache;

	@Inject
	private Provider<TypedModule> typedModuleProvider;

	public TypedModule getOrComputeTypes(Problem problem) {
		var resource = problem.eResource();
		return resourceScopeCache.get(CACHE_KEY, resource, () -> {
			var typedModule = typedModuleProvider.get();
			typedModule.setProblem(problem);
			return typedModule;
		});
	}

	public FixedType getExpressionType(Expr expr) {
		var problem = EcoreUtil2.getContainerOfType(expr, Problem.class);
		if (problem == null) {
			return ExprType.INVALID;
		}
		var typedModule = getOrComputeTypes(problem);
		return typedModule.getExpressionType(expr).getActualType();
	}
}
