/*******************************************************************************
 * Copyright (c) 2010-2015, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.objectives;

import tools.refinery.store.dse.DesignSpaceExplorationAdapter;

import java.util.Comparator;
import java.util.Objects;

/**
 * This abstract class implements the basic functionality of an objective ({@link Objective} namely its name,
 * comparator, level and fitness hard constraint.
 *
 * @author Andras Szabolcs Nagy
 *
 */
public abstract class BaseObjective implements Objective {

	protected final String name;
	protected Comparator<Double> comparator = Comparators.HIGHER_IS_BETTER;

	protected double fitnessConstraint;
	protected boolean isThereFitnessConstraint = false;
	protected Comparator<Double> fitnessConstraintComparator;

	public BaseObjective(String name) {
		Objects.requireNonNull(name, "Name of the objective cannot be null.");
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setComparator(Comparator<Double> comparator) {
		this.comparator = comparator;
	}

	@Override
	public Comparator<Double> getComparator() {
		return comparator;
	}

	public BaseObjective withComparator(Comparator<Double> comparator) {
		setComparator(comparator);
		return this;
	}

	/**
	 * Adds a hard constraint on the fitness value. For example, the fitness value must be better than 10 to accept the
	 * current state as a solution.
	 *
	 * @param fitnessConstraint
	 *            Solutions should be better than this value.
	 * @param fitnessConstraintComparator
	 *            {@link Comparator} to determine if the current state is better than the given value.
	 * @return The actual instance to enable builder pattern like usage.
	 */
	public BaseObjective withHardConstraintOnFitness(double fitnessConstraint,
													 Comparator<Double> fitnessConstraintComparator) {
		this.fitnessConstraint = fitnessConstraint;
		this.fitnessConstraintComparator = fitnessConstraintComparator;
		this.isThereFitnessConstraint = true;
		return this;
	}

	/**
	 * Adds a hard constraint on the fitness value. For example, the fitness value must be better than 10 to accept the
	 * current state as a solution. The provided comparator will be used.
	 *
	 * @param fitnessConstraint
	 *            Solutions should be better than this value.
	 * @return The actual instance to enable builder pattern like usage.
	 */
	public BaseObjective withHardConstraintOnFitness(double fitnessConstraint) {
		return withHardConstraintOnFitness(fitnessConstraint, null);
	}

	@Override
	public void init(DesignSpaceExplorationAdapter context) {
		if (fitnessConstraintComparator == null) {
			fitnessConstraintComparator = comparator;
		}
	}

	@Override
	public boolean isHardObjective() {
		return isThereFitnessConstraint;
	}

	@Override
	public boolean satisfiesHardObjective(Double fitness) {
		if (isThereFitnessConstraint) {
			int compare = fitnessConstraintComparator.compare(fitness, fitnessConstraint);
			if (compare < 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof BaseObjective baseObjective) {
			return name.equals(baseObjective.getName());
		}
		return false;
	}

	@Override
	public String toString() {
		return name;
	}

}
