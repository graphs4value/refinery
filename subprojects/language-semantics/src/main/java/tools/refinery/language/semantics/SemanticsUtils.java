/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.IScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.naming.ProblemQualifiedNameProvider;

import java.util.Optional;

@Singleton
public class SemanticsUtils {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	@Named(ProblemQualifiedNameProvider.NAMED_DELEGATE)
	private IQualifiedNameProvider delegateQualifiedNameProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	public Optional<String> getNameWithoutRootPrefix(EObject eObject) {
		var qualifiedName = delegateQualifiedNameProvider.getFullyQualifiedName(eObject);
		if (qualifiedName == null) {
			return Optional.empty();
		}
		return Optional.of(qualifiedNameConverter.toString(qualifiedName));
	}

	@Nullable
	public <T> T maybeGetElement(Problem problem, IScope scope, QualifiedName qualifiedName, Class<T> type) {
		if (qualifiedName == null) {
			throw new IllegalArgumentException("Element name must not be null");
		}
		var iterator = scope.getElements(qualifiedName).iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		var eObjectDescription = iterator.next();
		if (iterator.hasNext()) {
			var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
			throw new IllegalArgumentException("Ambiguous %s: %s"
					.formatted(type.getName(), qualifiedNameString));
		}
		var eObject = EcoreUtil.resolve(eObjectDescription.getEObjectOrProxy(), problem);
		if (!type.isInstance(eObject)) {
			var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
			throw new IllegalArgumentException("Not a %s: %s"
					.formatted(type.getName(), qualifiedNameString));
		}
		return type.cast(eObject);
	}

	@NotNull
	public <T> T getElement(Problem problem, IScope scope, QualifiedName qualifiedName, Class<T> type) {
		var element = maybeGetElement(problem, scope, qualifiedName, type);
		if (element == null) {
			var qualifiedNameString = qualifiedNameConverter.toString(qualifiedName);
			throw new IllegalArgumentException("No such %s: %s"
					.formatted(type.getName(), qualifiedNameString));
		}
		return element;
	}
}
