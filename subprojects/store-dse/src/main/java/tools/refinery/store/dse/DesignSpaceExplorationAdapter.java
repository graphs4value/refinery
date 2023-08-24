/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.internal.TransformationRule;
import tools.refinery.store.map.Version;
import tools.refinery.store.dse.internal.Activation;
import tools.refinery.store.dse.internal.DesignSpaceExplorationBuilderImpl;
import tools.refinery.store.dse.objectives.Fitness;
import tools.refinery.store.dse.objectives.ObjectiveComparatorHelper;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public interface DesignSpaceExplorationAdapter extends ModelAdapter {
	@Override
	DesignSpaceExplorationStoreAdapter getStoreAdapter();

	static DesignSpaceExplorationBuilder builder() {
		return new DesignSpaceExplorationBuilderImpl();
	}

	Collection<Version> explore();

	public int getModelSize();

	public Tuple1 createObject();

	public Tuple deleteObject(Tuple tuple);

	public boolean checkGlobalConstraints();

	public boolean backtrack();

	public boolean backtrack(String reason);

	public Fitness getFitness();

	public void newSolution();

	public int getDepth();

	public Collection<Activation> getUntraversedActivations();

	public boolean fireActivation(Activation activation);

	public boolean fireRandomActivation();

	public List<Version> getTrajectory();

	public boolean isCurrentStateAlreadyTraversed();

	public ObjectiveComparatorHelper getObjectiveComparatorHelper();

	public void restoreTrajectory(List<Version>  trajectory);

	public void setRandom(Random random);

	public void setRandom(long seed);

	public List<Version> getSolutions();

	void addTransformationRule(TransformationRule transformationRule);
}
