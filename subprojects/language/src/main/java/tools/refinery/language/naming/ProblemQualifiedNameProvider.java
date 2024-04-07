/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.naming;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.utils.ProblemUtil;

@Singleton
public class ProblemQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	protected QualifiedName qualifiedName(Problem problem) {
		var qualifiedNameString = problem.getName();
		if (qualifiedNameString != null) {
			return NamingUtil.stripRootPrefix(qualifiedNameConverter.toQualifiedName(qualifiedNameString));
		}
		if (!ProblemUtil.isModule(problem)) {
			return null;
		}
		var resource = problem.eResource();
		if (resource == null) {
			return null;
		}
		var resourceUri = resource.getURI();
		if (resourceUri == null) {
			return null;
		}
		var resourceSet = resource.getResourceSet();
		if (resourceSet == null) {
			return null;
		}
		var adapter = importAdapterProvider.getOrInstall(resourceSet);
		// If a module has no explicitly specified name, return the qualified name it was resolved under.
		return adapter.getQualifiedName(resourceUri);
	}
}
