/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.standalone;

import com.google.inject.Injector;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.language.ProblemStandaloneSetup;
import tools.refinery.generator.ProblemLoader;

public final class StandaloneRefinery {
	private StandaloneRefinery() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Injector getInjector() {
		return LazyHolder.INJECTOR;
	}

	public static <T> T getInstance(Class<T> type) {
		return getInjector().getInstance(type);
	}

	public static ProblemLoader getProblemLoader() {
		return getInstance(ProblemLoader.class);
	}

	public static ModelGeneratorFactory getGeneratorFactory() {
		return getInstance(ModelGeneratorFactory.class);
	}

	public static ModelSemanticsFactory getSemanticsFactory() {
		return getInstance(ModelSemanticsFactory.class);
	}

	private static final class LazyHolder {
		private static final Injector INJECTOR = createInjector();

		private static Injector createInjector() {
			return new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		}
	}
}
