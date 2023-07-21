package tools.refinery.store.query.dse;

import tools.refinery.store.model.Model;

public interface Strategy {

	public void initStrategy(DesignSpaceExplorationAdapter designSpaceExplorationAdapter);

	public void explore();
}
