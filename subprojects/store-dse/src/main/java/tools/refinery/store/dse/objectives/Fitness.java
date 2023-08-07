/*******************************************************************************
 * Copyright (c) 2010-2014, Miklos Foldenyi, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.objectives;

import java.util.HashMap;

public class Fitness extends HashMap<String, Double> {

	private boolean satisfiesHardObjectives;

	public boolean isSatisfiesHardObjectives() {
		return satisfiesHardObjectives;
	}

	public void setSatisfiesHardObjectives(boolean satisfiesHardObjectives) {
		this.satisfiesHardObjectives = satisfiesHardObjectives;
	}

	@Override
	public String toString() {
		return super.toString() + " hardObjectives=" + satisfiesHardObjectives;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		return satisfiesHardObjectives == ((Fitness) other).satisfiesHardObjectives;
	}

	@Override
	public int hashCode() {
		int h = super.hashCode();
		h = h * 31 + (satisfiesHardObjectives ? 1 : 0);
		return h;
	}

}
