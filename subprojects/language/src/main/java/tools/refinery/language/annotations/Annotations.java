/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.naming.NamingUtil;

import java.util.*;
import java.util.stream.Stream;

public class Annotations {
	public static final QualifiedName REPEATABLE = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME
			.append(AnnotationUtil.REPEATABLE_NAME);
	public static final QualifiedName OPTIONAL = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME
			.append(AnnotationUtil.OPTIONAL_NAME);

	private final EObject annotatedElement;
	private final Map<QualifiedName, CollectedAnnotations> allAnnotations;

	Annotations(IQualifiedNameProvider qualifiedNameProvider, EObject annotatedElement,
				Collection<tools.refinery.language.model.problem.Annotation> annotations) {
		this.annotatedElement = annotatedElement;
		if (annotations.isEmpty()) {
			allAnnotations = Map.of();
			return;
		}
		allAnnotations = new LinkedHashMap<>();
		for (var annotation : annotations) {
			processAnnotation(qualifiedNameProvider, annotation);
		}
	}

	private void processAnnotation(IQualifiedNameProvider qualifiedNameProvider,
								   tools.refinery.language.model.problem.Annotation problemAnnotation) {
		var declaration = problemAnnotation.getDeclaration();
		if (declaration == null || declaration.eIsProxy()) {
			return;
		}
		var qualifiedNameWithRootPrefix = qualifiedNameProvider.getFullyQualifiedName(declaration);
		if (qualifiedNameWithRootPrefix == null) {
			return;
		}
		var qualifiedName = NamingUtil.stripRootPrefix(qualifiedNameWithRootPrefix);
		var collected = allAnnotations.computeIfAbsent(qualifiedName, ignored -> {
			boolean repeatable = AnnotationUtil.isRepeatable(declaration);
			return new CollectedAnnotations(new ArrayList<>(), repeatable);
		});
		var annotation = new Annotation(annotatedElement, qualifiedName, problemAnnotation);
		collected.instances().add(annotation);
	}

	public boolean hasAnnotation(QualifiedName annotationName) {
		return allAnnotations.containsKey(annotationName);
	}

	public Optional<Annotation> getAnnotation(QualifiedName annotationName) {
		var collected = allAnnotations.get(annotationName);
		if (collected == null) {
			return Optional.empty();
		}
		if (collected.repeatable()) {
			throw new IllegalArgumentException("Annotation '%s' is repeatable.".formatted(annotationName));
		}
		var instances = collected.instances();
		return instances.isEmpty() ? Optional.empty() : Optional.of(instances.getFirst());
	}

	public Stream<Annotation> getAnnotations(QualifiedName annotationName) {
		var collected = allAnnotations.get(annotationName);
		if (collected == null) {
			return Stream.empty();
		}
		return collected.instances().stream();
	}

	public Stream<Annotation> getAllAnnotations() {
		return allAnnotations.values().stream()
				.flatMap(collected -> collected.instances().stream());
	}
}
