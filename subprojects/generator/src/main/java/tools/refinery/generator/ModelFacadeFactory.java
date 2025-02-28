/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import tools.refinery.generator.impl.ConcreteModelFacade;
import tools.refinery.generator.impl.ModelFacadeImpl;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.util.CancellationToken;

// This class is used as a fluent builder, so it's not necessary to use the return value of all of its methods.
@SuppressWarnings("UnusedReturnValue")
public abstract sealed class ModelFacadeFactory<T extends ModelFacadeFactory<T>> permits ModelSemanticsFactory,
		ModelGeneratorFactory {
	@Inject
	private Provider<ModelInitializer> initializerProvider;

	@Inject
	private Provider<SolutionSerializer> solutionSerializerProvider;

	@Inject
	private Provider<MetadataCreator> metadataCreatorProvider;

	@Inject
	private RefineryDiagnostics diagnostics;

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

	protected RefineryDiagnostics getDiagnostics() {
		return diagnostics;
	}

	protected ModelFacadeImpl.Args createFacadeArgs(ModelInitializer initializer, ModelStoreBuilder storeBuilder) {
		var trace = initializer.getProblemTrace();
		ModelStore store;
		try {
			store = storeBuilder.build();
		} catch (TranslationException e) {
			throw diagnostics.wrapTranslationException(e, trace);
		} catch (TracedException e) {
			throw diagnostics.wrapTracedException(e);
		}
		return new ModelFacadeImpl.Args(trace, store, initializer.getModelSeed(), metadataCreatorProvider,
				diagnostics);
	}

	protected ConcreteModelFacade.Args createConcreteFacadeArgs(ModelInitializer initializer,
															ModelStoreBuilder storeBuilder) {
		return new ConcreteModelFacade.Args(createFacadeArgs(initializer, storeBuilder), solutionSerializerProvider,
				keepNonExistingObjects);
	}
}
