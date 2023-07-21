package tools.refinery.store.query.dse.objectives;

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
}
