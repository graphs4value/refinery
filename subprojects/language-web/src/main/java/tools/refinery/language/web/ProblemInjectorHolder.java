/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web;

import com.google.inject.Injector;
import org.eclipse.xtext.util.DisposableRegistry;

public class ProblemInjectorHolder {
	private final Injector injector;
	private final DisposableRegistry disposableRegistry;
	private boolean disposed;

	public ProblemInjectorHolder() {
		injector = new ProblemWebSetup().createInjectorAndDoEMFRegistration();
		disposableRegistry = injector.getInstance(DisposableRegistry.class);
    }

	public Injector getInjector() {
		if (disposed) {
			throw new IllegalStateException("Injector has already been disposed");
		}
		return injector;
	}

	public void dispose() {
		if (!disposed) {
			disposableRegistry.dispose();
			disposed = true;
		}
	}
}
