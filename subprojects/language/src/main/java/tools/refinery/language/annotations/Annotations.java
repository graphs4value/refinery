/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;

import java.util.Optional;
import java.util.stream.Stream;

public interface Annotations {
	EObject getAnnotatedElement();

	boolean hasAnnotation(QualifiedName annotationName);

	Optional<Annotation> getAnnotation(QualifiedName annotationName);

	Stream<Annotation> getAnnotations(QualifiedName annotationName);

	Stream<Annotation> getAllAnnotations();
}
