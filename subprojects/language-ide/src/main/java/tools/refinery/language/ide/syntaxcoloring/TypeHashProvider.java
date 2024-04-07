/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.ide.syntaxcoloring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.GlobalResourceDescriptionProvider;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.documentation.DocumentationCommentParser;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.language.utils.ProblemUtil;

import java.util.*;

@Singleton
public class TypeHashProvider {
	private static final String CACHE_KEY = "tools.refinery.language.ide.syntaxcoloring.TypeHashProvider";
	private static final int COLOR_COUNT = 10;

	@Inject
	private IResourceScopeCache resourceScopeCache;

	@Inject
	private IScopeProvider scopeProvider;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private ImportCollector importCollector;

	@Inject
	private GlobalResourceDescriptionProvider globalResourceDescriptionProvider;

	public String getTypeHash(Relation relation) {
		if (!(relation instanceof ClassDeclaration || relation instanceof EnumDeclaration) ||
				ProblemUtil.isBuiltIn(relation)) {
			return null;
		}
		var qualifiedName = qualifiedNameProvider.getFullyQualifiedName(relation);
		if (qualifiedName == null) {
			return null;
		}
		var qualifiedNameString = qualifiedNameConverter.toString(NamingUtil.addRootPrefix(qualifiedName));
		var problem = EcoreUtil2.getContainerOfType(relation, Problem.class);
		if (problem == null) {
			return null;
		}
		var cache = resourceScopeCache.get(CACHE_KEY, problem.eResource(), () -> computeHashes(problem));
		return cache.get(qualifiedNameString);
	}

	private Map<String, String> computeHashes(Problem problem) {
		var resourceDescriptions = getResourceDescriptions(problem);
		var map = new HashMap<String, String>();
		var qualifiedNameStrings = new TreeSet<String>();
		for (var resourceDescription : resourceDescriptions) {
			for (var description : resourceDescription.getExportedObjectsByType(ProblemPackage.Literals.RELATION)) {
				if (ProblemResourceDescriptionStrategy.COLOR_RELATION_TRUE.equals(
						description.getUserData(ProblemResourceDescriptionStrategy.COLOR_RELATION))) {
					var qualifiedNameString = qualifiedNameConverter.toString(description.getQualifiedName());
					var presetColor = getPresetColor(description);
					if (presetColor != null) {
						map.put(qualifiedNameString, presetColor);
					}
					qualifiedNameStrings.add(qualifiedNameString);
				}
			}
		}
		var stringList = new ArrayList<>(qualifiedNameStrings);
		int size = stringList.size();
		if (size != 0) {
			shuffleColors(size, stringList, map);
		}
		return Collections.unmodifiableMap(map);
	}

	private static void shuffleColors(int size, ArrayList<String> stringList, Map<String, String> map) {
		// The use of a non-cryptographic random generator is safe here, because we only use it to shuffle the color
		// IDs in a pseudo-random way. The shuffle depends on the size of the list of identifiers before padding to
		// make sure that adding a new class randomizes all color IDs.
		@SuppressWarnings("squid:S2245")
		var random = new Random(size);
		int padding = COLOR_COUNT - (size % COLOR_COUNT);
		for (int i = 0; i < padding; i++) {
			stringList.add(null);
		}
		size += padding;
		Collections.shuffle(stringList, random);
		for (int i = 0; i < size; i++) {
			var key = stringList.get(i);
			if (key != null) {
				int colorId = i % COLOR_COUNT;
				map.putIfAbsent(key, Integer.toString(colorId));
			}
		}
	}

	private List<IResourceDescription> getResourceDescriptions(Problem problem) {
		var resource = problem.eResource();
		if (resource == null) {
			return List.of();
		}
		var resourceDescriptions = new ArrayList<IResourceDescription>();
		var resourceDescription = globalResourceDescriptionProvider.getResourceDescription(resource);
		if (resourceDescription != null) {
			resourceDescriptions.add(resourceDescription);
		}
		var resourceSet = resource.getResourceSet();
		if (resourceSet != null) {
			for (var importedUri : importCollector.getAllImports(resource).toUriSet()) {
				var importedResource = resourceSet.getResource(importedUri, false);
				if (importedResource != null) {
					var importedResourceDescription = globalResourceDescriptionProvider.getResourceDescription(
							importedResource);
					if (importedResourceDescription != null) {
						resourceDescriptions.add(importedResourceDescription);
					}
				}
			}
		}
		return resourceDescriptions;
	}

	private String getPresetColor(IEObjectDescription description) {
		return description.getUserData(DocumentationCommentParser.COLOR_TAG);
	}
}
