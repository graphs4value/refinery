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

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;

import java.util.Comparator;

/**
 *
 * Implementation of this interface represents a single objective of the DSE problem, which can assess a solution
 * (trajectory) in a single number. It has a name and a comparator which orders two solution based on the calculated
 * value.
 * <p>
 * Objectives can be either hard or soft objectives. Hard objectives can be satisfied or unsatisfied. If all of the hard
 * objectives are satisfied on a single solution, then it is considered to be a valid (or goal) solution.
 * <p>
 * Certain objectives can have inner state for calculating the fitness value. In this case a new instance is necessary
 * for every new thread, and the {@code createNew} method should not return the same instance more than once.
 *
 * @author Andras Szabolcs Nagy
 *
 */
public interface Objective {

	/**
	 * Returns the name of the objective.
	 *
	 * @return The name of the objective.
	 */
	String getName();

	/**
	 * Sets the {@link Comparator} which is used to compare fitness (doubles). It determines whether the objective is to
	 * minimize or maximize (or minimize or maximize a delta from a given number).
	 *
	 * @param comparator The comparator.
	 */
	void setComparator(Comparator<Double> comparator);

	/**
	 * Returns a {@link Comparator} which is used to compare fitness (doubles). It determines whether the objective is
	 * to minimize or maximize (or minimize or maximize a delta from a given number).
	 *
	 * @return The comparator.
	 */
	Comparator<Double> getComparator();

	/**
	 * Calculates the value of the objective on a given solution (trajectory).
	 *
	 * @param context
	 *            The {@link DesignSpaceExplorationAdapter}
	 * @return The objective value in double.
	 */
	Double getFitness(DesignSpaceExplorationAdapter context);

	/**
	 * Initializes the objective. It is called exactly once for every thread starts.
	 *
	 * @param context
	 *            The {@link DesignSpaceExplorationAdapter}.
	 */
	void init(DesignSpaceExplorationAdapter context);

	/**
	 * Returns an instance of the {@link Objective}. If it returns the same instance, all the methods has to be thread
	 * save as they are called concurrently.
	 *
	 * @return An instance of the objective.
	 */
	Objective createNew();

	/**
	 * Returns true if the objective is a hard objective. In such a case the method
	 * {@link Objective#satisfiesHardObjective(Double)} is called.
	 *
	 * @return True if the objective is a hard objective.
	 * @see Objective#satisfiesHardObjective(Double)
	 * @see Objective
	 */
	boolean isHardObjective();

	/**
	 * Determines if the given fitness value satisfies the hard objective.
	 *
	 * @param fitness
	 *            The fitness value of a solution.
	 * @return True if it satisfies the hard objective or it is a soft constraint.
	 * @see Objective
	 */
	boolean satisfiesHardObjective(Double fitness);

}
