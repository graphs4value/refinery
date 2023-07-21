/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.recipe;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import tools.refinery.store.query.literal.Connectivity;

public class RefineryRecipesFactoryImpl extends EFactoryImpl implements RefineryRecipesFactory {
	public static RefineryRecipesFactory init() {
		try {
			var factory = (RefineryRecipesFactory) EPackage.Registry.INSTANCE.getEFactory(
					RefineryRecipesPackage.eNS_URI);
			if (factory != null) {
				return factory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new RefineryRecipesFactoryImpl();
	}

	@Override
	public EObject create(EClass eClass) {
		if (eClass.getClassifierID() == RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE) {
			return createRepresentativeElectionRecipe();
		} else {
			throw new IllegalArgumentException("The class '%s' is not a valid classifier".formatted(eClass.getName()));
		}
	}

	@Override
	public Object createFromString(EDataType eDataType, String stringValue) {
		if (eDataType.getClassifierID() == RefineryRecipesPackage.CONNECTIVITY) {
			return createConnectivityFromString(eDataType, stringValue);
		} else {
			throw new IllegalArgumentException("The datatype '%s' is not a valid classifier"
					.formatted(eDataType.getName()));
		}
	}

	@Override
	public String convertToString(EDataType eDataType, Object objectValue) {
		if (eDataType.getClassifierID() == RefineryRecipesPackage.CONNECTIVITY) {
			return convertConnectivityToString(eDataType, objectValue);
		} else {
			throw new IllegalArgumentException("The datatype '%s' is not a valid classifier"
					.formatted(eDataType.getName()));
		}
	}

	@Override
	public RepresentativeElectionRecipe createRepresentativeElectionRecipe() {
		return new RepresentativeElectionRecipeImpl();
	}

	@Override
	public Connectivity createConnectivityFromString(EDataType eDataType, String initialValue) {
		return (Connectivity) super.createFromString(eDataType, initialValue);
	}

	@Override
	public String convertConnectivityToString(EDataType eDataType, Object instanceValue) {
		return super.convertToString(eDataType, instanceValue);
	}
}
