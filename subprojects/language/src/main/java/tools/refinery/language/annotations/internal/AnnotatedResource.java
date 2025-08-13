/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;

import java.util.*;

class AnnotatedResource {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	private final Map<EObject, TypedAnnotations> annotationsMap = new LinkedHashMap<>();
	private final Map<Annotation, TypedAnnotation> typingMap = new LinkedHashMap<>();

	public TypedAnnotations getTypedAnnotations(EObject annotatedElement) {
		var cachedResult = annotationsMap.get(annotatedElement);
		if (cachedResult != null) {
			return cachedResult;
		}
		var annotations = getProblemAnnotations(annotatedElement);
		if (annotations.isEmpty()) {
			// Do not store empty {@link Annotations} instances in the cache, as they can be trivially reconstructed.
			return new TypedAnnotations(annotatedElement, Map.of());
		}
		var collectedMap = LinkedHashMap.<QualifiedName, CollectedAnnotations>newLinkedHashMap(annotations.size());
		for (var annotation : annotations) {
			var typedAnnotation = getTypedAnnotationOfElement(annotatedElement, annotation);
			if (typedAnnotation == null) {
				continue;
			}
			var qualifiedName = typedAnnotation.getAnnotationName();
			var collectedAnnotations = collectedMap.computeIfAbsent(qualifiedName, ignored -> {
				boolean repeated = AnnotationUtil.isRepeatable(annotation.getDeclaration());
				return new CollectedAnnotations(new ArrayList<>(1), repeated);
			});
			collectedAnnotations.instances().add(typedAnnotation);
		}
		var typedAnnotations = new TypedAnnotations(annotatedElement, collectedMap);
		annotationsMap.put(annotatedElement, typedAnnotations);
		return typedAnnotations;
	}

	public Optional<TypedAnnotation> getTypedAnnotation(Annotation annotation) {
		var typedAnnotation = getTypedAnnotationOrNull(annotation);
		return Optional.ofNullable(typedAnnotation);
	}

	@Nullable
	private TypedAnnotation getTypedAnnotationOrNull(Annotation annotation) {
		if (annotation == null) {
			return null;
		}
		// We don't use {@code computeIfAbsent} here to avoid {@code ConcurrentModificationException} if this method
		// is called in a reentrant way.
		var result = typingMap.get(annotation);
		if (result == null) {
			result = computeTypedAnnotation(annotation);
			typingMap.put(annotation, result);
		}
		return result;
	}

	@Nullable
	private TypedAnnotation computeTypedAnnotation(Annotation annotation) {
		var annotatedElement = getAnnotatedElement(annotation);
		return computeTypedAnnotationOfElement(annotatedElement, annotation);
	}

	@Nullable
	private EObject getAnnotatedElement(Annotation annotation) {
		if (annotation.eContainer() instanceof TopLevelAnnotation) {
			return EcoreUtil2.getContainerOfType(annotation, Problem.class);
		}
		var annotatedElement = EcoreUtil2.getContainerOfType(annotation, AnnotatedElement.class);
		if (annotatedElement instanceof NodeDeclaration nodeDeclaration) {
			var nodes = nodeDeclaration.getNodes();
			if (nodes.isEmpty()) {
				return null;
			}
			// Annotated {@link NodeDeclaration} instances with multiple nodes are forbidden, so we can just use the
			// first node.
			return nodes.getFirst();
		}
		return annotatedElement;
	}

	@Nullable
	private TypedAnnotation getTypedAnnotationOfElement(EObject annotatedElement, Annotation annotation) {
		var cached = getTypedAnnotationOrNull(annotation);
		if (cached == null || Objects.equals(annotatedElement, cached.getAnnotatedElement())) {
			return cached;
		}
		// Create a new {@link TypedAnnotations} with the proper annotatedElement if the cache entry cannot be
		// reused because the annotations is shared across multiple nodes in a {@code NodeDeclaration}.
		// This will not appear in valid models, as annotated {@link NodeDeclaration} instances with multiple nodes
		// are forbidden.
		return computeTypedAnnotationOfElement(annotatedElement, annotation);
	}

	@Nullable
	private TypedAnnotation computeTypedAnnotationOfElement(EObject annotatedElement, Annotation annotation) {
		var declaration = annotation.getDeclaration();
		if (declaration == null || declaration.eIsProxy()) {
			return null;
		}
		var qualifiedNameWithRootPrefix = qualifiedNameProvider.getFullyQualifiedName(declaration);
		if (qualifiedNameWithRootPrefix == null) {
			return null;
		}
		var qualifiedName = NamingUtil.stripRootPrefix(qualifiedNameWithRootPrefix);
		return new TypedAnnotation(annotatedElement, qualifiedName, annotation);
	}

	private static List<Annotation> getProblemAnnotations(EObject eObject) {
		return switch (eObject) {
			case Problem problem -> getProblemAnnotations(problem);
			case AnnotatedElement annotatedElement -> getProblemAnnotations(annotatedElement);
			default -> List.of();
		};
	}

	private static List<Annotation> getProblemAnnotations(Problem problem) {
		return problem.getStatements().stream()
				.filter(TopLevelAnnotation.class::isInstance)
				.map(statement -> ((TopLevelAnnotation) statement).getAnnotation())
				.filter(Objects::nonNull)
				.toList();
	}

	private static List<Annotation> getProblemAnnotations(Node node) {
		var annotations = getAnnotationsFromContainer(node);
		var nodeDeclaration = EcoreUtil2.getContainerOfType(node, NodeDeclaration.class);
		var declarationAnnotations = nodeDeclaration == null ? List.<Annotation>of() :
				getAnnotationsFromContainer(nodeDeclaration);
		if (declarationAnnotations.isEmpty()) {
			return annotations;
		}
		if (annotations.isEmpty()) {
			return declarationAnnotations;
		}
		// This will not appear in valid models, as annotations can either be placed on the node declaration or the
		// node itself, but not at both places.
		var allAnnotations = new ArrayList<Annotation>(annotations.size() + declarationAnnotations.size());
		allAnnotations.addAll(annotations);
		allAnnotations.addAll(declarationAnnotations);
		return allAnnotations;
	}

	private static List<Annotation> getProblemAnnotations(AnnotatedElement annotatedElement) {
		return switch (annotatedElement) {
			case Node node -> getProblemAnnotations(node);
			// Annotations of {@code NodeDeclaration} instances apply to each of its nodes, not the declaration.
			case NodeDeclaration ignored -> List.of();
			default -> getAnnotationsFromContainer(annotatedElement);
		};
	}

	private static List<Annotation> getAnnotationsFromContainer(AnnotatedElement annotatedElement) {
		var annotationsContainer = annotatedElement.getAnnotations();
		if (annotationsContainer == null) {
			return List.of();
		}
		return annotationsContainer.getAnnotations();
	}
}
