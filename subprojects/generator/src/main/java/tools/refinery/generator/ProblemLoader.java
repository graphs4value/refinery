/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.LazyStringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.store.util.CancellationToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ProblemLoader {
	private String fileExtension;

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	private IResourceFactory resourceFactory;

	@Inject
	private IResourceValidator resourceValidator;

	private CancellationToken cancellationToken = CancellationToken.NONE;

	@Inject
	public void setFileExtensionProvider(FileExtensionProvider fileExtensionProvider) {
		this.fileExtension = fileExtensionProvider.getPrimaryFileExtension();
	}

	public ProblemLoader cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return this;
	}

	public Problem loadString(String problemString) throws IOException {
		try (var stream = new LazyStringInputStream(problemString)) {
			return loadStream(stream);
		}
	}

	public Problem loadStream(InputStream inputStream) throws IOException {
		var resourceSet = resourceSetProvider.get();
		var uri = URI.createFileURI("__synthetic." + fileExtension);
		var resource = resourceFactory.createResource(uri);
		resourceSet.getResources().add(resource);
		resource.load(inputStream, Map.of());
		return loadResource(resource);
	}

	public Problem loadFile(File file) throws IOException {
		return loadFile(file.getAbsolutePath());
	}

	public Problem loadFile(String filePath) throws IOException {
		return loadUri(URI.createFileURI(filePath));
	}

	public Problem loadUri(URI uri) throws IOException {
		var resourceSet = resourceSetProvider.get();
		var resource = resourceFactory.createResource(uri);
		resourceSet.getResources().add(resource);
		resource.load(Map.of());
		return loadResource(resource);
	}

	public Problem loadResource(Resource resource) {
		var issues = resourceValidator.validate(resource, CheckMode.ALL, () -> {
			cancellationToken.checkCancelled();
			return Thread.interrupted();
		});
		cancellationToken.checkCancelled();
		var errors = issues.stream()
				.filter(issue -> issue.getSeverity() == Severity.ERROR)
				.toList();
		if (!errors.isEmpty()) {
			throw new ValidationErrorsException(resource.getURI(), errors);
		}
		if (resource.getContents().isEmpty() || !(resource.getContents().getFirst() instanceof Problem problem)) {
			throw new IllegalArgumentException("Model generation problem not found in resource " + resource.getURI());
		}
		return problem;
	}
}
