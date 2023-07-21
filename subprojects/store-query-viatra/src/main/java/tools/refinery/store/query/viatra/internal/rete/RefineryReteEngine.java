/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryBackendContext;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteEngine;
import org.eclipse.viatra.query.runtime.rete.matcher.TimelyConfiguration;
import org.eclipse.viatra.query.runtime.rete.network.Network;
import org.eclipse.viatra.query.runtime.rete.network.NodeProvisioner;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class RefineryReteEngine extends ReteEngine {
	private static final MethodHandle REFINERY_NODE_FACTORY_CONSTRUCTOR;
	private static final MethodHandle REFINERY_CONNECTION_FACTORY_CONSTRUCTOR;
	private static final MethodHandle NETWORK_NODE_FACTORY_SETTER;
	private static final MethodHandle RETE_CONTAINER_CONNECTION_FACTORY_SETTER;
	private static final MethodHandle NODE_PROVISIONER_NODE_FACTORY_SETTER;
	private static final MethodHandle NODE_PROVISIONER_CONNECTION_FACTORY_SETTER;

	static {
		MethodHandles.Lookup lookup;
		try {
			lookup = MethodHandles.privateLookupIn(Network.class, MethodHandles.lookup());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot create private lookup", e);
		}
		var refineryNodeFactoryClass = defineClassFromFile(lookup, "RefineryNodeFactory");
		var refinaryConnectionFactoryClass = defineClassFromFile(lookup, "RefineryConnectionFactory");
		try {
			REFINERY_NODE_FACTORY_CONSTRUCTOR = lookup.findConstructor(refineryNodeFactoryClass,
					MethodType.methodType(Void.TYPE, Logger.class));
			REFINERY_CONNECTION_FACTORY_CONSTRUCTOR = lookup.findConstructor(refinaryConnectionFactoryClass,
					MethodType.methodType(Void.TYPE, ReteContainer.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot get constructor", e);
		}
		var nodeFactoryClass = refineryNodeFactoryClass.getSuperclass();
		var connectionFactoryClass = refinaryConnectionFactoryClass.getSuperclass();
		try {
			NETWORK_NODE_FACTORY_SETTER = lookup.findSetter(Network.class, "nodeFactory", nodeFactoryClass);
			RETE_CONTAINER_CONNECTION_FACTORY_SETTER = lookup.findSetter(ReteContainer.class, "connectionFactory",
					connectionFactoryClass);
			NODE_PROVISIONER_NODE_FACTORY_SETTER = lookup.findSetter(NodeProvisioner.class, "nodeFactory",
					nodeFactoryClass);
			NODE_PROVISIONER_CONNECTION_FACTORY_SETTER = lookup.findSetter(NodeProvisioner.class, "connectionFactory",
					connectionFactoryClass);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot get field setter", e);
		}
	}

	private static Class<?> defineClassFromFile(MethodHandles.Lookup lookup, String name) {
		byte[] classBytes;
		try (var resource = Network.class.getResourceAsStream(name + ".class")) {
			if (resource == null) {
				throw new IllegalStateException("Cannot find %s class file".formatted(name));
			}
			classBytes = resource.readAllBytes();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read %s class file".formatted(name), e);
		}
		Class<?> clazz;
		try {
			clazz = lookup.defineClass(classBytes);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot define %s class".formatted(name), e);
		}
		return clazz;
	}

	public RefineryReteEngine(IQueryBackendContext context, int reteThreads, boolean deleteAndReDeriveEvaluation,
							  TimelyConfiguration timelyConfiguration) {
		super(context, reteThreads, deleteAndReDeriveEvaluation, timelyConfiguration);
		installFactories();
	}

	private void installFactories() {
		var logger = getLogger();
		Object nodeFactory;
		try {
			nodeFactory = REFINERY_NODE_FACTORY_CONSTRUCTOR.invoke(logger);
		} catch (Error e) {
			// Fatal JVM errors should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Cannot construct node factory", e);
		}
		try {
			NETWORK_NODE_FACTORY_SETTER.invoke(reteNet, nodeFactory);
		} catch (Error e) {
			// Fatal JVM errors should not be wrapped.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Cannot set factory", e);
		}
		for (var container : reteNet.getContainers()) {
			Object connectionFactory;
			try {
				connectionFactory = REFINERY_CONNECTION_FACTORY_CONSTRUCTOR.invoke(container);
			} catch (Error e) {
				// Fatal JVM errors should not be wrapped.
				throw e;
			} catch (Throwable e) {
				throw new IllegalStateException("Cannot construct connection factory", e);
			}
			var provisioner = container.getProvisioner();
			try {
				RETE_CONTAINER_CONNECTION_FACTORY_SETTER.invoke(container, connectionFactory);
				NODE_PROVISIONER_NODE_FACTORY_SETTER.invoke(provisioner, nodeFactory);
				NODE_PROVISIONER_CONNECTION_FACTORY_SETTER.invoke(provisioner, connectionFactory);
			} catch (Error e) {
				// Fatal JVM errors should not be wrapped.
				throw e;
			} catch (Throwable e) {
				throw new IllegalStateException("Cannot set factory", e);
			}
		}
	}

	@Override
	public IQueryBackendFactory getFactory() {
		return RefineryReteBackendFactory.INSTANCE;
	}
}
