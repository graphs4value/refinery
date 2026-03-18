/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.annotations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.Annotation;
import tools.refinery.language.annotations.AnnotationContext;
import tools.refinery.language.annotations.Annotations;
import tools.refinery.language.model.problem.Problem;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class TopLevelAnnotations implements Annotations {
	private final Problem annotatedElement;
	private final Collection<Annotations> allAnnotations;

	public TopLevelAnnotations(AnnotationContext context, Problem problem, Collection<Problem> importedProblems) {
		annotatedElement = problem;
		allAnnotations = importedProblems.stream()
				.map(context::annotationsFor)
				.toList();
	}

	@Override
	public EObject getAnnotatedElement() {
		return annotatedElement;
	}

	@Override
	public boolean hasAnnotation(QualifiedName annotationName) {
		for (var annotations : allAnnotations) {
			if (annotations.hasAnnotation(annotationName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Optional<Annotation> getAnnotation(QualifiedName annotationName) {
		for (var annotations : allAnnotations) {
			var result = annotations.getAnnotation(annotationName);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}

	@Override
	public Stream<Annotation> getAnnotations(QualifiedName annotationName) {
		return allAnnotations.stream()
				.flatMap(annotations -> annotations.getAnnotations(annotationName));
	}

	@Override
	public Stream<Annotation> getAllAnnotations() {
		return allAnnotations.stream()
				.flatMap(Annotations::getAllAnnotations);
	}
}
