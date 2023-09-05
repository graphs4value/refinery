package tools.refinery.store.dse.tests;

import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.objectives.ObjectiveCalculator;
import tools.refinery.store.model.Model;

public class DummyObjective implements Objective {

	@Override
	public ObjectiveCalculator createCalculator(Model model) {
		return () -> {return 0d;};
	}
}
