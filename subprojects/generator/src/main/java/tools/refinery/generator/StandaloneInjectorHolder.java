/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Injector;
import tools.refinery.language.ProblemStandaloneSetup;

public final class StandaloneInjectorHolder {
	private StandaloneInjectorHolder() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Injector getInjector() {
		return LazyHolder.INJECTOR;
	}

	private static final class LazyHolder {
		private static final Injector INJECTOR = createInjector();

		private static Injector createInjector() {
			return new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		}
	}
}
