/*******************************************************************************
 * Copyright (c) 2010-2016, Andras Szabolcs Nagy, Zoltan Ujhelyi and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.objectives;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;

/**
 * This hard objective is fulfilled in any circumstances. Use it if all states should be regarded as a valid solution.
 *
 * @author Andras Szabolcs Nagy
 *
 */
public class AlwaysSatisfiedDummyHardObjective extends BaseObjective {

	private static final String DEFAULT_NAME = "AlwaysSatisfiedDummyHardObjective";

	public AlwaysSatisfiedDummyHardObjective() {
		super(DEFAULT_NAME);
	}

	public AlwaysSatisfiedDummyHardObjective(String name) {
		super(name);
	}

	@Override
	public Double getFitness(DesignSpaceExplorationAdapter context) {
		return 0d;
	}

	@Override
	public boolean isHardObjective() {
		return true;
	}

	@Override
	public boolean satisfiesHardObjective(Double fitness) {
		return true;
	}

	@Override
	public Objective createNew() {
		return this;
	}

}
