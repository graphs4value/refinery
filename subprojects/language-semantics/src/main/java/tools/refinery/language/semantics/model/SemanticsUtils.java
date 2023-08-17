/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;

import java.util.Optional;

@Singleton
public class SemanticsUtils {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	public Optional<String> getName(EObject eObject) {
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(eObject);
		if (qualifiedName == null) {
			return Optional.empty();
		}
		return Optional.of(qualifiedNameConverter.toString(qualifiedName));
	}
}
