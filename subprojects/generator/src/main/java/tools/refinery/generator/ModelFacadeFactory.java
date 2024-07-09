/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.store.util.CancellationToken;

public abstract sealed class ModelFacadeFactory<T extends ModelFacadeFactory<T>> permits ModelSemanticsFactory,
		ModelGeneratorFactory {
	@Inject
	private Provider<ModelInitializer> initializerProvider;

	private CancellationToken cancellationToken = CancellationToken.NONE;

	private boolean keepNonExistingObjects = false;

	protected abstract T getSelf();

	public T cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return getSelf();
	}

	public T keepNonExistingObjects(boolean removeNonExistentObjects) {
		this.keepNonExistingObjects = removeNonExistentObjects;
		return getSelf();
	}

	protected ModelInitializer createModelInitializer() {
		return initializerProvider.get();
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
}
