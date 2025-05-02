/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Singleton
public class ClassHierarchyCollector {
	private static final String CLASS_HIERARCHY =
			"tools.refinery.language.validation.ClassHierarchyCollector.CLASS_HIERARCHY";

	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	public Collection<Relation> getSuperTypes(ClassDeclaration classDeclaration) {
		var resource = classDeclaration.eResource();
		if (resource == null) {
			return doGetSuperTypes(classDeclaration);
		}
		return cache.get(Tuples.create(classDeclaration, CLASS_HIERARCHY), resource,
				() -> doGetSuperTypes(classDeclaration));
	}

	private Collection<Relation> doGetSuperTypes(ClassDeclaration classDeclaration) {
		var allSuperTypes = new LinkedHashSet<Relation>();
		var visited = new LinkedHashSet<ClassDeclaration>();
		var queue = new ArrayDeque<ClassDeclaration>();
		queue.add(classDeclaration);
		while (!queue.isEmpty()) {
			var current = queue.poll();
            if (!visited.add(current)) {
				continue;
			}
            var superTypes = current.getSuperTypes();
			for (var superType : superTypes) {
				allSuperTypes.add(superType);
                if (superType instanceof ClassDeclaration superClassDeclaration) {
                    queue.add(superClassDeclaration);
                }
            }
		}
		if (classDeclaration.eResource() != null) {
			var builtinSymbols = importAdapterProvider.getBuiltinSymbols(classDeclaration);
			allSuperTypes.add(builtinSymbols.node());
		}
		return List.copyOf(allSuperTypes);
	}
}
