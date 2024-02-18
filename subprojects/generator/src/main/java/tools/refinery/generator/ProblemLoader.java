/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.resource.*;
import org.eclipse.xtext.scoping.impl.GlobalResourceDescriptionProvider;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.LazyStringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.ScopeDeclaration;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy.ShadowingKey;
import tools.refinery.language.scoping.imports.ImportAdapter;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.store.util.CancellationToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

// This class is used as a fluent builder.
@SuppressWarnings("UnusedReturnValue")
public class ProblemLoader {
	private String fileExtension;

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	private IResourceFactory resourceFactory;

	@Inject
	private IResourceValidator resourceValidator;

	@Inject
	private ImportCollector importCollector;

	@Inject
	private GlobalResourceDescriptionProvider globalResourceDescriptionProvider;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	private CancellationToken cancellationToken = CancellationToken.NONE;

	private final List<Path> extraPaths = new ArrayList<>();

	@Inject
	public void setFileExtensionProvider(FileExtensionProvider fileExtensionProvider) {
		this.fileExtension = fileExtensionProvider.getPrimaryFileExtension();
	}

	public ProblemLoader cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return this;
	}

	public ProblemLoader extraPath(String path) {
		return extraPath(Path.of(path));
	}

	public ProblemLoader extraPath(Path path) {
		extraPaths.add(path.toAbsolutePath().normalize());
		return this;
	}

	public Problem loadString(String problemString, URI uri) throws IOException {
		try (var stream = new LazyStringInputStream(problemString)) {
			return loadStream(stream, uri);
		}
	}

	public Problem loadString(String problemString) throws IOException {
		return loadString(problemString, null);
	}

	public Problem loadStream(InputStream inputStream, URI uri) throws IOException {
		var resourceSet = createResourceSet();
		var resourceUri = uri == null ? URI.createURI("__synthetic." + fileExtension) : uri;
		var resource = resourceFactory.createResource(resourceUri);
		resourceSet.getResources().add(resource);
		resource.load(inputStream, Map.of());
		return loadResource(resource);
	}

	public Problem loadStream(InputStream inputStream) throws IOException {
		return loadStream(inputStream, null);
	}

	public Problem loadFile(File file) throws IOException {
		return loadFile(file.getAbsolutePath());
	}

	public Problem loadFile(String filePath) throws IOException {
		return loadUri(URI.createFileURI(filePath));
	}

	public Problem loadUri(URI uri) throws IOException {
		var resourceSet = createResourceSet();
		var resource = resourceFactory.createResource(uri);
		resourceSet.getResources().add(resource);
		resource.load(Map.of());
		return loadResource(resource);
	}

	private XtextResourceSet createResourceSet() {
		var resourceSet = resourceSetProvider.get();
		var adapter = ImportAdapter.getOrInstall(resourceSet);
		adapter.getLibraryPaths().addAll(0, extraPaths);
		return resourceSet;
	}

	public Problem loadResource(Resource resource) {
		EcoreUtil.resolveAll(resource);
		CancelIndicator cancelIndicator = () -> {
			cancellationToken.checkCancelled();
			return Thread.interrupted();
		};
		var shadowedNames = new LinkedHashMap<ShadowingKey, Set<IEObjectDescription>>();
		var issues = new ArrayList<Issue>();
		validateResource(resource, issues, cancelIndicator);
		cancellationToken.checkCancelled();
		var resourceSet = resource.getResourceSet();
		if (resourceSet != null) {
			var imports = importCollector.getAllImports(resource).toUriSet();
			cancellationToken.checkCancelled();
			for (var importedUri : imports) {
				var importedResource = resourceSet.getResource(importedUri, false);
				if (importedResource == null) {
					throw new IllegalStateException("Unknown imported resource: " + importedUri);
				}
				findShadowedNames(importedResource, shadowedNames);
				validateResource(importedResource, issues, cancelIndicator);
			}
		}
		addNameClashIssues(issues, shadowedNames);
		var errors = issues.stream().filter(issue -> issue.getSeverity() == Severity.ERROR).toList();
		if (!errors.isEmpty()) {
			throw new ValidationErrorsException(resource.getURI(), errors);
		}
		if (resource.getContents().isEmpty() || !(resource.getContents().getFirst() instanceof Problem problem)) {
			throw new IllegalArgumentException("Model generation problem not found in resource " + resource.getURI());
		}
		return problem;
	}

	private void findShadowedNames(Resource importedResource,
								   LinkedHashMap<ShadowingKey, Set<IEObjectDescription>> shadowedNames) {
		var resourceDescription = globalResourceDescriptionProvider.getResourceDescription(importedResource);
		for (var eObjectDescription : resourceDescription.getExportedObjects()) {
			var name = eObjectDescription.getName();
			if (NamingUtil.isFullyQualified(name)) {
				var shadowingKey = ProblemResourceDescriptionStrategy.getShadowingKey(eObjectDescription);
				var entries = shadowedNames.computeIfAbsent(shadowingKey, ignored -> new LinkedHashSet<>());
				entries.add(eObjectDescription);
			}
		}
		cancellationToken.checkCancelled();
	}

	private void validateResource(Resource importedResource, ArrayList<Issue> issues,
								  CancelIndicator cancelIndicator) {
		issues.addAll(resourceValidator.validate(importedResource, CheckMode.ALL, cancelIndicator));
		cancellationToken.checkCancelled();
	}

	private void addNameClashIssues(ArrayList<Issue> issues,
									LinkedHashMap<ShadowingKey, Set<IEObjectDescription>> shadowedNames) {
		for (var entry : shadowedNames.entrySet()) {
			var eObjectDescriptions = entry.getValue();
			if (eObjectDescriptions.size() <= 1) {
				continue;
			}
			var qualifiedName = qualifiedNameConverter.toString(NamingUtil.stripRootPrefix(entry.getKey().name()));
			var uris = eObjectDescriptions.stream()
					.map(eObjectDescription -> eObjectDescription.getEObjectURI().trimFragment().toString())
					.collect(Collectors.joining(", "));
			var message = "Object with qualified name %s is also defined in %s".formatted(qualifiedName, uris);
			for (var eObjectDescription : eObjectDescriptions) {
				var issue = new Issue.IssueImpl();
				issue.setSeverity(Severity.ERROR);
				issue.setMessage(message);
				issue.setUriToProblem(eObjectDescription.getEObjectURI());
				issues.add(issue);
			}
		}
	}

	public Problem loadScopeConstraints(Problem problem, List<String> extraScopes, List<String> overrideScopes) throws IOException {
		var allScopes = new ArrayList<>(extraScopes);
		allScopes.addAll(overrideScopes);
		if (allScopes.isEmpty()) {
			return problem;
		}
		int originalStatementCount = problem.getStatements().size();
		var builder = new StringBuilder();
		var problemResource = problem.eResource();
		try (var outputStream = new ByteArrayOutputStream()) {
			problemResource.save(outputStream, Map.of());
			builder.append(outputStream.toString(StandardCharsets.UTF_8));
		}
		builder.append('\n');
		for (var scope : allScopes) {
			builder.append("scope ").append(scope).append(".\n");
		}
		var modifiedProblem = loadString(builder.toString(), problemResource.getURI());
		var modifiedStatements = modifiedProblem.getStatements();
		int modifiedStatementCount = modifiedStatements.size();
		if (modifiedStatementCount != originalStatementCount + allScopes.size()) {
			throw new IllegalArgumentException("Failed to parse scope constraints");
		}
		// Override scopes remove any scope constraint from the original problem with the same target type.
		var overriddenScopes = new HashSet<Relation>();
		for (int i = modifiedStatementCount - overrideScopes.size(); i < modifiedStatementCount; i++) {
			var statement = modifiedStatements.get(i);
			if (!(statement instanceof ScopeDeclaration scopeDeclaration)) {
				throw new IllegalStateException("Invalid scope constraint: " + statement);
			}
			for (var typeScope : scopeDeclaration.getTypeScopes()) {
				overriddenScopes.add(typeScope.getTargetType());
			}
		}
		int statementIndex = 0;
		var iterator = modifiedStatements.iterator();
		// Scope overrides only affect type scopes from the original problem and leave type scopes added on the
		// command line intact.
		while (statementIndex < originalStatementCount && iterator.hasNext()) {
			var statement = iterator.next();
			if (statement instanceof ScopeDeclaration scopeDeclaration) {
				var typeScopes = scopeDeclaration.getTypeScopes();
				typeScopes.removeIf(typeScope -> overriddenScopes.contains(typeScope.getTargetType()));
				// Scope declarations with no type scopes are invalid, so we have to remove them.
				if (typeScopes.isEmpty()) {
					iterator.remove();
				}
			}
			statementIndex++;
		}
		return modifiedProblem;
	}
}
