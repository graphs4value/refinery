/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.library.RefineryLibraries;
import tools.refinery.language.model.problem.ImportStatement;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.resource.LoadOnDemandResourceDescriptionProvider;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;

import java.util.*;

@Singleton
public class ImportCollector {
	private static final String PREFIX = "tools.refinery.language.imports.";
	private static final String DIRECT_IMPORTS_KEY = PREFIX + "DIRECT_IMPORTS";
	private static final String ALL_IMPORTS_KEY = PREFIX + "ALL_IMPORTS";

	@Inject
	private IResourceScopeCache cache;

	@Inject
	private LinkingHelper linkingHelper;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private Provider<LoadOnDemandResourceDescriptionProvider> loadOnDemandProvider;

	public ImportCollection getDirectImports(Resource resource) {
		return cache.get(DIRECT_IMPORTS_KEY, resource, () -> this.computeDirectImports(resource));
	}

	protected ImportCollection computeDirectImports(Resource resource) {
		if (resource.getContents().isEmpty() || !(resource.getContents().getFirst() instanceof Problem problem)) {
			return ImportCollection.EMPTY;
		}
		Map<QualifiedName, Set<QualifiedName>> aliasesMap = new LinkedHashMap<>();
		for (var statement : problem.getStatements()) {
			if (statement instanceof ImportStatement importStatement) {
				collectImportStatement(importStatement, aliasesMap);
			}
		}
		var collection = new ImportCollection();
		collection.addAll(RefineryLibraries.getAutomaticImports());
		for (var entry : aliasesMap.entrySet()) {
			var qualifiedName = entry.getKey();
			RefineryLibraries.resolveQualifiedName(qualifiedName).ifPresent(uri -> {
				if (!uri.equals(resource.getURI())) {
					var aliases = entry.getValue();
					collection.add(NamedImport.explicit(uri, qualifiedName, List.copyOf(aliases)));
				}
			});
		}
		collection.remove(resource.getURI());
		return collection;
	}

	private void collectImportStatement(ImportStatement importStatement, Map<QualifiedName, Set<QualifiedName>> aliasesMap) {
		var nodes = NodeModelUtils.findNodesForFeature(importStatement,
				ProblemPackage.Literals.IMPORT_STATEMENT__IMPORTED_MODULE);
		var aliasString = importStatement.getAlias();
		var alias = Strings.isNullOrEmpty(aliasString) ? QualifiedName.EMPTY :
				NamingUtil.stripRootPrefix(qualifiedNameConverter.toQualifiedName(aliasString));
		for (var node : nodes) {
			var qualifiedNameString = linkingHelper.getCrossRefNodeAsString(node, true);
			if (Strings.isNullOrEmpty(qualifiedNameString)) {
				continue;
			}
			var qualifiedName = NamingUtil.stripRootPrefix(
					qualifiedNameConverter.toQualifiedName(qualifiedNameString));
			var aliases = aliasesMap.computeIfAbsent(qualifiedName, ignored -> new LinkedHashSet<>());
			aliases.add(alias);
		}
	}

	public ImportCollection getAllImports(Resource resource) {
		return cache.get(ALL_IMPORTS_KEY, resource, () -> this.computeAllImports(resource));
	}

	protected ImportCollection computeAllImports(Resource resource) {
		var collection = new ImportCollection();
		collection.addAll(getDirectImports(resource).toList());
		var loadOnDemand = loadOnDemandProvider.get();
		loadOnDemand.setContext(resource);
		var seen = new HashSet<URI>();
		seen.add(resource.getURI());
		var queue = new ArrayDeque<>(collection.toUriSet());
		while (!queue.isEmpty()) {
			var uri = queue.removeFirst();
			seen.add(uri);
			collection.add(new TransitiveImport(uri));
			var resourceDescription = loadOnDemand.getResourceDescription(uri);
			if (resourceDescription == null) {
				continue;
			}
			var problemDescriptions = resourceDescription.getExportedObjectsByType(ProblemPackage.Literals.PROBLEM);
			for (var eObjectDescription : problemDescriptions) {
				for (var importedUri : getImports(eObjectDescription)) {
					if (!seen.contains(importedUri)) {
						queue.addLast(importedUri);
					}
				}
			}
		}
		collection.remove(resource.getURI());
		return collection;
	}

	protected List<URI> getImports(IEObjectDescription eObjectDescription) {
		var importString = eObjectDescription.getUserData(ProblemResourceDescriptionStrategy.IMPORTS);
		if (importString == null) {
			return List.of();
		}
		return Splitter.on(ProblemResourceDescriptionStrategy.IMPORTS_SEPARATOR).splitToStream(importString)
				.map(URI::createURI)
				.toList();
	}
}
