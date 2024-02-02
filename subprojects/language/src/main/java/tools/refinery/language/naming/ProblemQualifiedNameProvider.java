/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.naming;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;

public class ProblemQualifiedNameProvider extends IQualifiedNameProvider.AbstractImpl {
	private static final String PREFIX = "tools.refinery.language.naming.ProblemQualifiedNameProvider.";
	public static final String NAMED_DELEGATE = PREFIX + "NAMED_DELEGATE";
	public static final String CACHE_KEY = PREFIX + "CACHE_KEY";

	@Inject
	@Named(NAMED_DELEGATE)
	private IQualifiedNameProvider delegate;

	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;

	@Override
	public QualifiedName getFullyQualifiedName(EObject obj) {
		return cache.get(Tuples.pair(obj, CACHE_KEY), obj.eResource(), () -> computeFullyQualifiedName(obj));
	}

	public QualifiedName computeFullyQualifiedName(EObject obj) {
		var qualifiedName = delegate.getFullyQualifiedName(obj);
		if (qualifiedName != null && ProblemResourceDescriptionStrategy.shouldExport(obj)) {
			return NamingUtil.addRootPrefix(qualifiedName);
		}
		return qualifiedName;
	}
}
