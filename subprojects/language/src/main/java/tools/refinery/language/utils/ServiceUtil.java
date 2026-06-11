/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import com.google.inject.Injector;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class ServiceUtil {
	private static final Logger LOG = Logger.getLogger(ServiceUtil.class);

	private ServiceUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static <T> List<Class<? extends T>> loadServices(Class<T> serviceClass) {
		return ServiceLoader.load(serviceClass).stream()
				.<Class<? extends T>>mapMulti((provider, consumer) -> {
					Class<? extends T> implementationClass = null;
					try {
						implementationClass = provider.type();
					} catch (ServiceConfigurationError e) {
						LOG.error("Error loading service: " + serviceClass.getName(), e);
					}
					if (implementationClass != null) {
						consumer.accept(implementationClass);
					}
				})
				.toList();
	}

	public static <T> List<T> instantiate(Injector injector, List<Class<? extends T>> implementationClasses) {
		var instances = new ArrayList<T>(implementationClasses.size());
		for (var implementationClass : implementationClasses) {
			T instance = null;
			try {
				instance = injector.getInstance(implementationClass);
			} catch (RuntimeException e) {
				LOG.error("Error loading service: " + implementationClass.getName(), e);
			}
			if (instance != null) {
				instances.add(instance);
			}
		}
		return instances;
	}
}
