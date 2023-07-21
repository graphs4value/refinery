/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.recipe;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.viatra.query.runtime.rete.recipes.RecipesPackage;
import tools.refinery.store.query.literal.Connectivity;

public class RefineryRecipesPackageImpl extends EPackageImpl implements RefineryRecipesPackage {
	private static boolean isInstanceInitialized;
	private boolean isCreated;
	private boolean isInitialized;
	private EClass representativeElectionRecipe;
	private EDataType connectivity;

	public static RefineryRecipesPackage init() {
		if (isInstanceInitialized) {
			return (RefineryRecipesPackage) Registry.INSTANCE.getEPackage(eNS_URI);
		}
		var thePackage = Registry.INSTANCE.get(eNS_URI) instanceof RefineryRecipesPackageImpl impl ? impl :
				new RefineryRecipesPackageImpl();
		isInstanceInitialized = true;
		thePackage.createPackageContents();
		thePackage.initializePackageContents();
		thePackage.freeze();
		Registry.INSTANCE.put(eNS_URI, thePackage);
		return thePackage;
	}

	private RefineryRecipesPackageImpl() {
		super(eNS_URI, RefineryRecipesFactory.eINSTANCE);
	}

	@Override
	public EClass getRepresentativeElectionRecipe() {
		return representativeElectionRecipe;
	}

	@Override
	public EAttribute getRepresentativeElectionRecipe_Connectivity() {
		return (EAttribute) representativeElectionRecipe.getEStructuralFeatures().get(0);
	}

	@Override
	public EOperation getRepresentativeElectionRecipe_GetArity() {
		return representativeElectionRecipe.getEOperations().get(0);
	}

	@Override
	public EDataType getConnectivity() {
		return connectivity;
	}

	public void createPackageContents() {
		if (isCreated) {
			return;
		}
		isCreated = true;

		representativeElectionRecipe = createEClass(REPRESENTATIVE_ELECTION_RECIPE);
		createEAttribute(representativeElectionRecipe, REPRESENTATIVE_ELECTION_RECIPE__CONNECTIVITY);
		createEOperation(representativeElectionRecipe, REPRESENTATIVE_ELECTION_RECIPE__GET_ARITY);

		connectivity = createEDataType(CONNECTIVITY);
	}

	public void initializePackageContents() {
		if (isInitialized) {
			return;
		}
		isInitialized = true;

		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		representativeElectionRecipe.getESuperTypes().add(RecipesPackage.Literals.ALPHA_RECIPE);

		initEClass(representativeElectionRecipe, RepresentativeElectionRecipe.class,
				"RepresentativeElectionRecipe", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRepresentativeElectionRecipe_Connectivity(), getConnectivity(), "connectivity", null, 0, 1,
				RepresentativeElectionRecipe.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID,
				IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEOperation(getRepresentativeElectionRecipe_GetArity(), EcorePackage.Literals.EINT, "getArity", 0, 1,
				!IS_UNIQUE, IS_ORDERED);

		initEDataType(connectivity, Connectivity.class, "Connectivity", IS_SERIALIZABLE,
				!IS_GENERATED_INSTANCE_CLASS);

		createResource(eNS_URI);
	}
}
