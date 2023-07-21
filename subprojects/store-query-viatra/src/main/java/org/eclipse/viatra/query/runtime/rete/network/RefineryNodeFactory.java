/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.query.runtime.rete.network;

import org.apache.log4j.Logger;
import org.eclipse.viatra.query.runtime.rete.recipes.ReteNodeRecipe;
import org.eclipse.viatra.query.runtime.rete.traceability.TraceInfo;
import tools.refinery.store.query.viatra.internal.rete.network.RefineryNodeFactoryExtensions;

/**
 * This class overrides some RETE node types from {@link NodeFactory}.
 * <p>
 * Since {@link NodeFactory} is package-private, this class has to be in the
 * {@code org.eclipse.viatra.query.runtime.rete.network} package as well.
 * However, due to JAR signature verification errors, <b>this class cannot be loaded directly</b>
 * and has to be loaded at runtime as a byte array instead.
 */
@SuppressWarnings("unused")
class RefineryNodeFactory extends NodeFactory {
	private final RefineryNodeFactoryExtensions extensions = new RefineryNodeFactoryExtensions();

	public RefineryNodeFactory(Logger logger) {
		super(logger);
	}

	@Override
	public Supplier createNode(ReteContainer reteContainer, ReteNodeRecipe recipe, TraceInfo... traces) {
		var extendedResult = extensions.createNode(reteContainer, recipe, traces);
		if (extendedResult != null) {
			return extendedResult;
		}
		return super.createNode(reteContainer, recipe, traces);
	}
}
