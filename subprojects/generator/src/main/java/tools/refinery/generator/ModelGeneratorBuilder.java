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
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.LazyStringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import tools.refinery.generator.impl.ProblemTraceImpl;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.model.ModelInitializer;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.util.CancellationToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ModelGeneratorBuilder {
	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	private IResourceFactory resourceFactory;

	@Inject
	private IResourceValidator resourceValidator;

	@Inject
	private ModelInitializer initializer;

	@Inject
	private ProblemTraceImpl problemTrace;

	private CancellationToken cancellationToken = CancellationToken.NONE;
	private boolean problemLoaded;
	private ModelQueryBuilder queryEngineBuilder;
	private Collection<Concreteness> requiredInterpretations = Set.of(Concreteness.CANDIDATE);

	public ModelGeneratorBuilder cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return this;
	}

	public ModelGeneratorBuilder fromString(String problemString) throws IOException {
		try (var stream = new LazyStringInputStream(problemString)) {
			return fromStream(stream);
		}
	}

	public ModelGeneratorBuilder fromStream(InputStream inputStream) throws IOException {
		var resourceSet = resourceSetProvider.get();
		var resource = resourceFactory.createResource(URI.createFileURI("__synthetic.problem"));
		resourceSet.getResources().add(resource);
		resource.load(inputStream, Map.of());
		return fromResource(resource);
	}

	public ModelGeneratorBuilder fromFile(File file) throws IOException {
		return fromFile(file.getAbsolutePath());
	}

	public ModelGeneratorBuilder fromFile(String filePath) throws IOException {
		return fromUri(URI.createFileURI(filePath));
	}

	public ModelGeneratorBuilder fromUri(URI uri) throws IOException {
		var resourceSet = resourceSetProvider.get();
		var resource = resourceFactory.createResource(uri);
		resourceSet.getResources().add(resource);
		resource.load(Map.of());
		return fromResource(resource);
	}

	public ModelGeneratorBuilder fromResource(Resource resource) {
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
		if (resource.getContents().isEmpty() || !(resource.getContents().get(0) instanceof Problem problem)) {
			throw new IllegalArgumentException("Model generation problem not found in resource " + resource.getURI());
		}
		return problem(problem);
	}

	public ModelGeneratorBuilder problem(Problem problem) {
		if (problemLoaded) {
			throw new IllegalStateException("Problem was already set");
		}
		initializer.readProblem(problem);
		problemTrace.setInitializer(initializer);
		problemLoaded = true;
		return this;
	}

	public ProblemTrace getProblemTrace() {
		checkProblem();
		return problemTrace;
	}

	public ModelGeneratorBuilder queryEngine(ModelQueryBuilder queryEngineBuilder) {
		if (this.queryEngineBuilder != null) {
			throw new IllegalStateException("Query engine was already set");
		}
		this.queryEngineBuilder = queryEngineBuilder;
		return this;
	}

	public ModelGeneratorBuilder requiredInterpretations(Collection<Concreteness> requiredInterpretations) {
		this.requiredInterpretations = requiredInterpretations;
		return this;
	}

	public ModelGenerator build() {
		checkProblem();
		var storeBuilder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(getQueryEngineBuilder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(requiredInterpretations));
		initializer.configureStoreBuilder(storeBuilder);
		var store = storeBuilder.build();
		return new ModelGenerator(problemTrace, store, initializer.getModelSeed());
	}

	private void checkProblem() {
		if (!problemLoaded) {
			throw new IllegalStateException("Problem was not loaded");
		}
	}

	private ModelQueryBuilder getQueryEngineBuilder() {
		if (queryEngineBuilder == null) {
			return QueryInterpreterAdapter.builder();
		}
		return queryEngineBuilder;
	}
}
