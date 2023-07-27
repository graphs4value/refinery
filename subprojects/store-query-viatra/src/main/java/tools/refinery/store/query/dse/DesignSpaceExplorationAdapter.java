package tools.refinery.store.query.dse;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.query.dse.internal.Activation;
import tools.refinery.store.query.dse.internal.DesignSpaceExplorationBuilderImpl;
import tools.refinery.store.query.dse.objectives.Fitness;
import tools.refinery.store.query.dse.objectives.ObjectiveComparatorHelper;
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

	Collection<Long> explore();

	public int getModelSize();

	public Tuple1 createObject();

	public Tuple deleteObject(Tuple tuple);

	public boolean checkGlobalConstraints();

	public boolean backtrack();

	public Fitness calculateFitness();

	public void newSolution();

	public int getDepth();

	public Collection<Activation> getUntraversedActivations();

	public boolean fireActivation(Activation activation);

	public void fireRandomActivation();

	public boolean isCurrentInTrajectory();

	public List<Long> getTrajectory();

	public boolean isCurrentStateAlreadyTraversed();

	public ObjectiveComparatorHelper getObjectiveComparatorHelper();

	public void restoreTrajectory(List<Long>  trajectory);

	public void setRandom(Random random);

	public void setRandom(long seed);
}
