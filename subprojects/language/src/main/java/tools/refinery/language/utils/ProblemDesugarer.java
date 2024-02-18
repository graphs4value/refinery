/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;

import java.util.*;

@Singleton
public class ProblemDesugarer {
	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;

	public Optional<Problem> getBuiltinProblem(EObject context) {
		return Optional.ofNullable(context).map(EObject::eResource).flatMap(resource ->
				cache.get("builtinProblem", resource, () -> doGetBuiltinProblem(resource)));
	}

	private Optional<Problem> doGetBuiltinProblem(Resource resource) {
		return Optional.ofNullable(resource).map(Resource::getResourceSet)
				.map(resourceSet -> resourceSet.getResource(BuiltinLibrary.BUILTIN_LIBRARY_URI, true))
				.map(Resource::getContents).filter(contents -> !contents.isEmpty()).map(List::getFirst)
				.filter(Problem.class::isInstance).map(Problem.class::cast);
	}

	public Optional<BuiltinSymbols> getBuiltinSymbols(EObject context) {
		return getBuiltinProblem(context).map(builtin ->
				cache.get("builtinSymbols", builtin.eResource(), () -> doGetBuiltinSymbols(builtin)));
	}

	private BuiltinSymbols doGetBuiltinSymbols(Problem builtin) {
		var node = doGetDeclaration(builtin, ClassDeclaration.class, "node");
		var equals = doGetDeclaration(builtin, PredicateDefinition.class, "equals");
		var exists = doGetDeclaration(builtin, PredicateDefinition.class, "exists");
		var contained = doGetDeclaration(builtin, ClassDeclaration.class, "contained");
		var contains = doGetDeclaration(builtin, PredicateDefinition.class, "contains");
		var invalidContainer = doGetDeclaration(builtin, PredicateDefinition.class, "invalidContainer");
		return new BuiltinSymbols(builtin, node, equals, exists, contained, contains, invalidContainer);
	}

	private <T extends Statement & NamedElement> T doGetDeclaration(Problem builtin, Class<T> type, String name) {
		return builtin.getStatements().stream().filter(type::isInstance).map(type::cast)
				.filter(declaration -> name.equals(declaration.getName())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Built-in declaration " + name + " was not found"));
	}

	public Collection<ClassDeclaration> getSuperclassesAndSelf(ClassDeclaration classDeclaration) {
		return cache.get(Tuples.create(classDeclaration, "superclassesAndSelf"), classDeclaration.eResource(),
				() -> doGetSuperclassesAndSelf(classDeclaration));
	}

	private Collection<ClassDeclaration> doGetSuperclassesAndSelf(ClassDeclaration classDeclaration) {
		var builtinSymbols = getBuiltinSymbols(classDeclaration);
		Set<ClassDeclaration> found = new HashSet<>();
		builtinSymbols.ifPresent(symbols -> found.add(symbols.node()));
		Deque<ClassDeclaration> queue = new ArrayDeque<>();
		queue.addLast(classDeclaration);
		while (!queue.isEmpty()) {
			ClassDeclaration current = queue.removeFirst();
			if (!found.contains(current)) {
				found.add(current);
				for (Relation superType : current.getSuperTypes()) {
					if (superType instanceof ClassDeclaration superDeclaration) {
						queue.addLast(superDeclaration);
					}
				}
			}
		}
		return found;
	}

	public Collection<ReferenceDeclaration> getAllReferenceDeclarations(ClassDeclaration classDeclaration) {
		return cache.get(Tuples.create(classDeclaration, "allReferenceDeclarations"), classDeclaration.eResource(),
				() -> doGetAllReferenceDeclarations(classDeclaration));
	}

	private Collection<ReferenceDeclaration> doGetAllReferenceDeclarations(ClassDeclaration classDeclaration) {
		Set<ReferenceDeclaration> referenceDeclarations = new HashSet<>();
		for (ClassDeclaration superclass : getSuperclassesAndSelf(classDeclaration)) {
			for (FeatureDeclaration featureDeclaration : superclass.getFeatureDeclarations()) {
				if (featureDeclaration instanceof ReferenceDeclaration referenceDeclaration) {
					referenceDeclarations.add(referenceDeclaration);
				}
			}
		}
		return referenceDeclarations;
	}

	public boolean isContainmentReference(ReferenceDeclaration referenceDeclaration) {
		return referenceDeclaration.getKind() == ReferenceKind.CONTAINMENT;
	}
}
