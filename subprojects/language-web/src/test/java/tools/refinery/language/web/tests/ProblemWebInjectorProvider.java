/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.tests;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.xtext.ide.ExecutorServiceProvider;
import org.eclipse.xtext.util.DisposableRegistry;
import org.eclipse.xtext.util.Modules2;
import tools.refinery.language.ide.ProblemIdeModule;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.language.web.ProblemWebModule;
import tools.refinery.language.web.ProblemWebSetup;

import java.util.concurrent.ExecutorService;

public class ProblemWebInjectorProvider extends ProblemInjectorProvider {

	protected Injector internalCreateInjector() {
		return new ProblemWebSetup() {
			@Override
			public Injector createInjector() {
				return Guice.createInjector(
						Modules2.mixin(createRuntimeModule(), new ProblemIdeModule(), createWebModule()));
			}
		}.createInjectorAndDoEMFRegistration();
	}

	protected ProblemWebModule createWebModule() {
		// Await termination of the executor service to avoid race conditions between
		// the tasks in the service and the {@link
		// org.eclipse.xtext.testing.extensions.InjectionExtension}.
		return new ProblemWebModule() {
			@Override
			public Class<? extends ExecutorServiceProvider> bindExecutorServiceProvider() {
				return DelegatingExecutorServiceProvider.class;
			}

			@Override
			public void configureExecutorServiceProvider(Binder binder) {
				binder.bind(ExecutorService.class).toProvider(DelegatingExecutorServiceProvider.class);
			}
		};
	}

	@Override
	public void restoreRegistry() {
		// Also make sure to dispose any IDisposable instances (that may depend on the
		// global state) created by Xtext before restoring the global state.
		var disposableRegistry = getInjector().getInstance(DisposableRegistry.class);
		disposableRegistry.dispose();
		super.restoreRegistry();
	}
}
