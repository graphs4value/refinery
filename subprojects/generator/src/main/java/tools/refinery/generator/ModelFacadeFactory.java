/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.FeatureBasedDiagnostic;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.util.CancellationToken;

import java.util.ArrayList;

// This class is used as a fluent builder, so it's not necessary to use the return value of all of its methods.
@SuppressWarnings("UnusedReturnValue")
public abstract sealed class ModelFacadeFactory<T extends ModelFacadeFactory<T>> permits ModelSemanticsFactory,
		ModelGeneratorFactory {
	private static final String DIAGNOSTIC_ID = "tools.refinery.generator.ModelFacade.SEMANTIC_ERROR";

	@Inject
	private Provider<ModelInitializer> initializerProvider;

	@Inject
	private Provider<SolutionSerializer> solutionSerializerProvider;

	@Inject
	private Provider<MetadataCreator> metadataCreatorProvider;

	@Inject
	private IDiagnosticConverter diagnosticConverter;

	private CancellationToken cancellationToken = CancellationToken.NONE;

	private boolean keepNonExistingObjects;

	private boolean keepShadowPredicates = true;

	protected abstract T getSelf();

	public T cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return getSelf();
	}

	public T keepNonExistingObjects(boolean keepNonExistentObjects) {
		this.keepNonExistingObjects = keepNonExistentObjects;
		return getSelf();
	}

	public T keepShadowPredicates(boolean keepShadowPredicates) {
		this.keepShadowPredicates = keepShadowPredicates;
		return getSelf();
	}

	protected ModelInitializer createModelInitializer() {
		var initializer = initializerProvider.get();
		initializer.setKeepNonExistingObjects(keepNonExistingObjects);
		initializer.setKeepShadowPredicates(keepShadowPredicates);
		return initializer;
	}

	protected CancellationToken getCancellationToken() {
		return cancellationToken;
	}

	protected boolean isKeepNonExistingObjects() {
		return keepNonExistingObjects;
	}

	protected void checkCancelled() {
		cancellationToken.checkCancelled();
	}

	protected Provider<SolutionSerializer> getSolutionSerializerProvider() {
		return solutionSerializerProvider;
	}

	protected Provider<MetadataCreator> getMetadataCreatorProvider() {
		return metadataCreatorProvider;
	}

	protected ModelStore buildWithTrace(ModelStoreBuilder storeBuilder, ProblemTrace trace) {
		try {
			return storeBuilder.build();
		} catch (TranslationException e) {
			var problem = trace.getProblem();
			var problemUri = problem.eResource().getURI();
			var partialSymbol = e.getPartialSymbol();
			if (partialSymbol == null) {
				throw createInvalidProblemException(problemUri, problem, e);
			}
			var relation = trace.getRelation(partialSymbol);
			if (relation == null) {
				throw createInvalidProblemException(problemUri, problem, e);
			}
			throw createInvalidProblemException(problemUri, relation, e);
		} catch (TracedException e) {
			var problemUri = trace.getProblem().eResource().getURI();
			throw createInvalidProblemException(problemUri, e.getSourceElement(), e);
		}
	}

	protected InvalidProblemException createInvalidProblemException(URI resourceUri, EObject sourceElement,
																	Throwable cause) {
		var diagnostic = new FeatureBasedDiagnostic(Diagnostic.ERROR, cause.getMessage(), sourceElement, null,
				0, CheckType.EXPENSIVE, DIAGNOSTIC_ID);
		var issues = new ArrayList<Issue>(1);
		diagnosticConverter.convertValidatorDiagnostic(diagnostic, issues::add);
		return new InvalidProblemException(resourceUri, issues, cause);
	}
}
