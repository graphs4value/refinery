/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.recipe;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.viatra.query.runtime.rete.recipes.RecipesPackage;
import org.eclipse.viatra.query.runtime.rete.recipes.ReteNodeRecipe;
import org.eclipse.viatra.query.runtime.rete.recipes.impl.AlphaRecipeImpl;
import tools.refinery.store.query.literal.Connectivity;

import java.lang.reflect.InvocationTargetException;

public class RepresentativeElectionRecipeImpl extends AlphaRecipeImpl implements RepresentativeElectionRecipe {
	private Connectivity connectivity;

	@Override
	protected EClass eStaticClass() {
		return RefineryRecipesPackage.eINSTANCE.getRepresentativeElectionRecipe();
	}

	@Override
	public Connectivity getConnectivity() {
		return connectivity;
	}

	@Override
	public void setConnectivity(Connectivity newStrong) {
		var oldConnectivity = connectivity;
		connectivity = newStrong;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE__CONNECTIVITY, oldConnectivity,
					connectivity));
		}
	}

	@Override
	public int getArity() {
		return 2;
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		if (featureID == RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE__CONNECTIVITY) {
			return getConnectivity();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@Override
	public void eSet(int featureID, Object newValue) {
		if (featureID == RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE__CONNECTIVITY) {
			setConnectivity((Connectivity) newValue);
		} else {
			super.eSet(featureID, newValue);
		}
	}

	@Override
	public void eUnset(int featureID) {
		if (featureID == RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE__CONNECTIVITY) {
			setConnectivity(null);
		} else {
			super.eUnset(featureID);
		}
	}

	@Override
	public boolean eIsSet(int featureID) {
		if (featureID == RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE__CONNECTIVITY) {
			return connectivity != null;
		}
		return super.eIsSet(featureID);
	}

	@Override
	public int eDerivedOperationID(int baseOperationID, Class<?> baseClass) {
		if (baseClass == ReteNodeRecipe.class && baseOperationID == RecipesPackage.RETE_NODE_RECIPE___GET_ARITY) {
			return RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE__GET_ARITY;
		}
		return super.eDerivedOperationID(baseOperationID, baseClass);
	}

	@Override
	public Object eInvoke(int operationID, EList<?> arguments) throws InvocationTargetException {
		if (operationID == RefineryRecipesPackage.REPRESENTATIVE_ELECTION_RECIPE__GET_ARITY) {
			return getArity();
		}
		return super.eInvoke(operationID, arguments);
	}

	@Override
	public String toString() {
		if (eIsProxy()) {
			return super.toString();
		}
		return "%s (connectivity: %s)".formatted(super.toString(), connectivity);
	}
}
