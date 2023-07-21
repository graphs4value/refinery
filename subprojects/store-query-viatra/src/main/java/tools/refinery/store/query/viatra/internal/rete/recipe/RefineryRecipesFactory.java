/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.recipe;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import tools.refinery.store.query.literal.Connectivity;

// Naming and index computation follows EMF conventions.
@SuppressWarnings("squid:S115")
public interface RefineryRecipesFactory extends EFactory {
	RefineryRecipesFactory eINSTANCE = RefineryRecipesFactoryImpl.init();

	RepresentativeElectionRecipe createRepresentativeElectionRecipe();

	Connectivity createConnectivityFromString(EDataType eDataType, String initialValue);

	String convertConnectivityToString(EDataType eDataType, Object instanceValue);
}
