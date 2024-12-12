/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.Annotation;
import tools.refinery.language.annotations.Annotations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class TypedAnnotations implements Annotations {
	private final EObject annotatedElement;
	private final Map<QualifiedName, CollectedAnnotations> allAnnotations;

	TypedAnnotations(EObject annotatedElement, Map<QualifiedName, CollectedAnnotations> allAnnotations) {
		this.annotatedElement = annotatedElement;
		this.allAnnotations = allAnnotations;
	}

	@Override
	public EObject getAnnotatedElement() {
		return annotatedElement;
	}

	@Override
	public boolean hasAnnotation(QualifiedName annotationName) {
		return allAnnotations.containsKey(annotationName);
	}

	@Override
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

	@Override
	public Stream<Annotation> getAnnotations(QualifiedName annotationName) {
		var collected = allAnnotations.get(annotationName);
		if (collected == null) {
			return Stream.empty();
		}
		return collected.instances().stream();
	}

	@Override
	public Stream<Annotation> getAllAnnotations() {
		return allAnnotations.values().stream()
				.flatMap(collected -> collected.instances().stream());
	}

	public Map<String, List<Annotation>> getDuplicateAnnotations() {
		var duplicateAnnotations = LinkedHashMap.<String, List<Annotation>>newLinkedHashMap(0);
		for (var entry : allAnnotations.entrySet()) {
			var collected = entry.getValue();
			if (!collected.repeatable() && collected.instances().size() >= 2) {
				var name = entry.getKey().getLastSegment();
				duplicateAnnotations.put(name, List.copyOf(collected.instances()));
			}
		}
		return duplicateAnnotations;
	}
}
