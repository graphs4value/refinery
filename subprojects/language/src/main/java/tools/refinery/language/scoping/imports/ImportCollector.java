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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.util.IResourceScopeCache;
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
		var resourceSet = resource.getResourceSet();
		if (resourceSet == null) {
			return ImportCollection.EMPTY;
		}
		var adapter = ImportAdapter.getOrInstall(resourceSet);
		var collection = new ImportCollection();
		collectAutomaticImports(collection, adapter);
		collectExplicitImports(problem, collection, adapter);
		collection.remove(resource.getURI());
		return collection;
	}

	private void collectAutomaticImports(ImportCollection importCollection, ImportAdapter adapter) {
		for (var library : adapter.getLibraries()) {
			for (var qualifiedName : library.getAutomaticImports()) {
				var uri = adapter.resolveQualifiedName(qualifiedName);
				if (uri != null) {
					importCollection.add(NamedImport.implicit(uri, qualifiedName));
				}
			}
		}
	}

	private void collectExplicitImports(Problem problem, ImportCollection collection, ImportAdapter adapter) {
		for (var statement : problem.getStatements()) {
			if (statement instanceof ImportStatement importStatement) {
				collectImportStatement(importStatement, collection, adapter);
			}
		}
	}

	private void collectImportStatement(ImportStatement importStatement, ImportCollection collection,
										ImportAdapter adapter) {
		var nodes = NodeModelUtils.findNodesForFeature(importStatement,
				ProblemPackage.Literals.IMPORT_STATEMENT__IMPORTED_MODULE);
		var aliasString = importStatement.getAlias();
		var alias = Strings.isNullOrEmpty(aliasString) ? QualifiedName.EMPTY :
				NamingUtil.stripRootPrefix(qualifiedNameConverter.toQualifiedName(aliasString));
		var referredProblem = (EObject) importStatement.eGet(ProblemPackage.Literals.IMPORT_STATEMENT__IMPORTED_MODULE,
				false);
		URI referencedUri = null;
		if (referredProblem != null && !referredProblem.eIsProxy()) {
			var resource = referredProblem.eResource();
			if (resource != null) {
				referencedUri = resource.getURI();
			}
		}
		for (var node : nodes) {
			var qualifiedNameString = linkingHelper.getCrossRefNodeAsString(node, true);
			if (Strings.isNullOrEmpty(qualifiedNameString)) {
				continue;
			}
			var qualifiedName = NamingUtil.stripRootPrefix(
					qualifiedNameConverter.toQualifiedName(qualifiedNameString));
			var uri = referencedUri == null ? adapter.resolveQualifiedName(qualifiedName) : referencedUri;
			if (uri != null) {
				collection.add(NamedImport.explicit(uri, qualifiedName, List.of(alias)));
			}
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
		if (importString == null || importString.isEmpty()) {
			return List.of();
		}
		return Splitter.on(ProblemResourceDescriptionStrategy.IMPORTS_SEPARATOR).splitToStream(importString)
				.map(URI::createURI)
				.toList();
	}
}
